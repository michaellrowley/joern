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
      // Use the libadalang_parser.py script from resources
      val parserScript = getLibadalangParserScript()
      val command = Seq(
        "python3",
        parserScript.toString,
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

  private def getLibadalangParserScript(): Path = {
    // First, try to get the script from resources
    val resourceStream = getClass.getResourceAsStream("/libadalang_parser.py")
    
    if (resourceStream != null) {
      // Extract script from resources to a temporary location
      val tempScript = Files.createTempFile("libadalang_parser", ".py")
      try {
        val bytes = resourceStream.readAllBytes()
        Files.write(tempScript, bytes)
        tempScript.toFile.setExecutable(true)
        tempScript
      } finally {
        resourceStream.close()
      }
    } else {
      // Fallback: check if script exists in the frontend directory
      val frontendDir = Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI)
        .getParent.getParent.getParent.getParent
      val scriptPath = frontendDir.resolve("src/main/resources/libadalang_parser.py")
      
      if (Files.exists(scriptPath)) {
        scriptPath
      } else {
        throw new RuntimeException(
          "libadalang_parser.py not found. Please ensure it's in the resources directory."
        )
      }
    }
  }
}
