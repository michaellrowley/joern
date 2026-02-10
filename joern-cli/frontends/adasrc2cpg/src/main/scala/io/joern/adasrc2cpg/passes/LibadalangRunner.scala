package io.joern.adasrc2cpg.passes

import io.joern.adasrc2cpg.Config
import org.slf4j.LoggerFactory

import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters._
import scala.sys.process._
import scala.util.{Failure, Success, Try}

/** Runs libadalang to parse Ada source files and generate AST JSON
  */
class LibadalangRunner(config: Config) {
  private val logger = LoggerFactory.getLogger(getClass)

  /** Execute libadalang parser on Ada source files
    * @param outputDir directory where JSON AST files will be written
    * @return Option containing list of parsed JSON file paths
    */
  def execute(outputDir: Path): Option[List[Path]] = {
    val inputPath = Paths.get(config.inputPath)
    
    if (!Files.exists(inputPath)) {
      logger.error(s"Input path does not exist: ${config.inputPath}")
      return None
    }

    // Find all Ada source files
    val adaFiles = findAdaFiles(inputPath)
    
    if (adaFiles.isEmpty) {
      logger.warn(s"No Ada files found in: ${config.inputPath}")
      return Some(List.empty)
    }

    logger.info(s"Found ${adaFiles.size} Ada files to process")

    // Parse each Ada file with libadalang
    val parsedFiles = adaFiles.flatMap { adaFile =>
      parseAdaFile(adaFile, outputDir)
    }

    if (parsedFiles.isEmpty && adaFiles.nonEmpty) {
      logger.error("Failed to parse any Ada files")
      None
    } else {
      Some(parsedFiles)
    }
  }

  private def findAdaFiles(path: Path): List[Path] = {
    if (Files.isDirectory(path)) {
      Files.walk(path)
        .iterator()
        .asScala
        .filter(Files.isRegularFile(_))
        .filter(p => {
          val fileName = p.getFileName.toString.toLowerCase
          fileName.endsWith(".adb") || fileName.endsWith(".ads")
        })
        .toList
    } else if (Files.isRegularFile(path)) {
      val fileName = path.getFileName.toString.toLowerCase
      if (fileName.endsWith(".adb") || fileName.endsWith(".ads")) {
        List(path)
      } else {
        List.empty
      }
    } else {
      List.empty
    }
  }

  private def parseAdaFile(adaFile: Path, outputDir: Path): Option[Path] = {
    val outputFileName = adaFile.getFileName.toString + ".json"
    val outputFile = outputDir.resolve(outputFileName)

    try {
      // Call Python script to parse Ada file with libadalang
      val pythonScript = createLibadalangParserScript()
      val command = Seq(
        "python3",
        pythonScript.toString,
        adaFile.toString,
        outputFile.toString
      )

      logger.debug(s"Parsing ${adaFile.getFileName} with libadalang")
      
      val result = Try(command.!!)
      
      result match {
        case Success(_) if Files.exists(outputFile) =>
          logger.debug(s"Successfully parsed: ${adaFile.getFileName}")
          Some(outputFile)
        case Success(_) =>
          logger.error(s"Parser succeeded but output file not found: $outputFile")
          None
        case Failure(e) =>
          logger.error(s"Failed to parse ${adaFile.getFileName}: ${e.getMessage}")
          None
      }
    } catch {
      case e: Exception =>
        logger.error(s"Error parsing ${adaFile.getFileName}: ${e.getMessage}")
        None
    }
  }

  private def createLibadalangParserScript(): Path = {
    // Create a temporary Python script that uses libadalang
    val script = Files.createTempFile("libadalang_parser", ".py")
    
    val scriptContent = """#!/usr/bin/env python3
import sys
import json
try:
    import libadalang as lal
except ImportError:
    print("Error: libadalang is not installed. Install with: pip install libadalang", file=sys.stderr)
    sys.exit(1)

def node_to_dict(node):
    if node is None:
        return None
    
    result = {
        "kind": node.kind_name,
        "text": node.text if hasattr(node, 'text') else "",
        "sloc_range": {
            "start": {"line": node.sloc_range.start.line, "column": node.sloc_range.start.column},
            "end": {"line": node.sloc_range.end.line, "column": node.sloc_range.end.column}
        } if hasattr(node, 'sloc_range') and node.sloc_range else None,
        "children": []
    }
    
    for child in node:
        if child is not None:
            result["children"].append(node_to_dict(child))
    
    return result

def parse_ada_file(input_file, output_file):
    try:
        context = lal.AnalysisContext()
        unit = context.get_from_file(input_file)
        
        if unit.root is None:
            print(f"Error: Failed to parse {input_file}", file=sys.stderr)
            if unit.diagnostics:
                for diag in unit.diagnostics:
                    print(f"  {diag}", file=sys.stderr)
            return False
        
        ast_dict = {
            "file": input_file,
            "root": node_to_dict(unit.root)
        }
        
        with open(output_file, 'w') as f:
            json.dump(ast_dict, f, indent=2)
        
        return True
        
    except Exception as e:
        print(f"Error parsing {input_file}: {e}", file=sys.stderr)
        return False

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python3 script.py <input_ada_file> <output_json_file>", file=sys.stderr)
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    
    success = parse_ada_file(input_file, output_file)
    sys.exit(0 if success else 1)
"""
    
    Files.write(script, scriptContent.getBytes("UTF-8"))
    script.toFile.setExecutable(true)
    script
  }
}
