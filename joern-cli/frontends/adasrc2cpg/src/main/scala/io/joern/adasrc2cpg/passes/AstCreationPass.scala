package io.joern.adasrc2cpg.passes

import io.joern.adasrc2cpg.Config
import io.joern.x2cpg.passes.frontend.{TypeRecoveryParserConfig, XTypeRecovery}
import io.joern.x2cpg.utils.Report
import io.joern.x2cpg.{Ast, ValidationMode}
import io.shiftleft.codepropertygraph.generated.{Cpg, EdgeTypes, NodeTypes}
import io.shiftleft.codepropertygraph.generated.nodes._
import io.shiftleft.passes.ConcurrentWriterCpgPass
import org.slf4j.LoggerFactory
import upickle.default._

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

/** Creates CPG nodes from libadalang JSON AST files
  */
class AstCreationPass(cpg: Cpg, parsedFiles: List[Path], config: Config, report: Report)
    extends ConcurrentWriterCpgPass[Path](cpg) {
  
  private val logger = LoggerFactory.getLogger(getClass)

  override def generateParts(): Array[Path] = parsedFiles.toArray

  override def runOnPart(diffGraph: DiffGraphBuilder, jsonFile: Path): Unit = {
    try {
      val jsonContent = Files.readString(jsonFile)
      val astJson = read[AdaAst](jsonContent)
      
      logger.debug(s"Processing AST for file: ${astJson.file}")
      
      // Create FILE node
      val fileNode = NewFile()
        .name(astJson.file)
        .order(0)
      diffGraph.addNode(fileNode)
      
      // Create NAMESPACE_BLOCK
      val namespaceBlock = NewNamespaceBlock()
        .name(astJson.file)
        .fullName(astJson.file)
        .filename(astJson.file)
        .order(1)
      diffGraph.addNode(namespaceBlock)
      diffGraph.addEdge(fileNode, namespaceBlock, EdgeTypes.AST)
      
      // Process AST root
      if (astJson.root != null) {
        processNode(astJson.root, namespaceBlock, diffGraph)
      }
      
    } catch {
      case e: Exception =>
        logger.error(s"Failed to process JSON file ${jsonFile.getFileName}: ${e.getMessage}")
        report.addError(s"Failed to process ${jsonFile.getFileName}")
    }
  }

  private def processNode(node: AstNode, parent: StoredNode, diffGraph: DiffGraphBuilder): Unit = {
    // Map libadalang node kinds to CPG nodes
    node.kind match {
      case "SubpBody" | "SubpDecl" =>
        createMethodNode(node, parent, diffGraph)
      case "ObjectDecl" =>
        createLocalNode(node, parent, diffGraph)
      case "CallExpr" =>
        createCallNode(node, parent, diffGraph)
      case "Identifier" | "DefiningName" =>
        createIdentifierNode(node, parent, diffGraph)
      case _ =>
        // For other nodes, just process children
        node.children.foreach { child =>
          if (child != null) {
            processNode(child, parent, diffGraph)
          }
        }
    }
  }

  private def createMethodNode(node: AstNode, parent: StoredNode, diffGraph: DiffGraphBuilder): Unit = {
    val methodName = extractName(node)
    val method = NewMethod()
      .name(methodName)
      .fullName(methodName)
      .signature("")
      .filename(parent.property(NodeTypes.NAMESPACE_BLOCK.FILENAME, ""))
      .lineNumber(node.sloc_range.flatMap(_.start).map(_.line))
      .columnNumber(node.sloc_range.flatMap(_.start).map(_.column))
      .code(node.text.getOrElse(""))
    
    diffGraph.addNode(method)
    diffGraph.addEdge(parent, method, EdgeTypes.AST)
    
    // Process method body
    node.children.foreach { child =>
      if (child != null) {
        processNode(child, method, diffGraph)
      }
    }
  }

  private def createLocalNode(node: AstNode, parent: StoredNode, diffGraph: DiffGraphBuilder): Unit = {
    val varName = extractName(node)
    val local = NewLocal()
      .name(varName)
      .code(node.text.getOrElse(""))
      .typeFullName("ANY")
    
    diffGraph.addNode(local)
    diffGraph.addEdge(parent, local, EdgeTypes.AST)
  }

  private def createCallNode(node: AstNode, parent: StoredNode, diffGraph: DiffGraphBuilder): Unit = {
    val callName = extractName(node)
    val call = NewCall()
      .name(callName)
      .methodFullName(callName)
      .signature("")
      .typeFullName("ANY")
      .dispatchType("STATIC_DISPATCH")
      .code(node.text.getOrElse(""))
      .lineNumber(node.sloc_range.flatMap(_.start).map(_.line))
      .columnNumber(node.sloc_range.flatMap(_.start).map(_.column))
    
    diffGraph.addNode(call)
    diffGraph.addEdge(parent, call, EdgeTypes.AST)
    
    // Process call arguments
    node.children.foreach { child =>
      if (child != null) {
        processNode(child, call, diffGraph)
      }
    }
  }

  private def createIdentifierNode(node: AstNode, parent: StoredNode, diffGraph: DiffGraphBuilder): Unit = {
    val name = node.text.getOrElse("unknown")
    val identifier = NewIdentifier()
      .name(name)
      .code(name)
      .typeFullName("ANY")
      .lineNumber(node.sloc_range.flatMap(_.start).map(_.line))
      .columnNumber(node.sloc_range.flatMap(_.start).map(_.column))
    
    diffGraph.addNode(identifier)
    diffGraph.addEdge(parent, identifier, EdgeTypes.AST)
  }

  private def extractName(node: AstNode): String = {
    // Try to find a name in the node's children
    node.children.find(_.kind == "DefiningName")
      .flatMap(_.text)
      .orElse(node.text)
      .getOrElse("unknown")
  }
}

// JSON data structures for libadalang AST
case class AdaAst(file: String, root: AstNode)
case class AstNode(
  kind: String,
  text: Option[String],
  sloc_range: Option[SlocRange],
  children: List[AstNode]
)
case class SlocRange(start: Option[Position], end: Option[Position])
case class Position(line: Int, column: Int)

object AdaAst {
  implicit val positionRW: ReadWriter[Position] = macroRW
  implicit val slocRangeRW: ReadWriter[SlocRange] = macroRW
  implicit val astNodeRW: ReadWriter[AstNode] = macroRW
  implicit val adaAstRW: ReadWriter[AdaAst] = macroRW
}
