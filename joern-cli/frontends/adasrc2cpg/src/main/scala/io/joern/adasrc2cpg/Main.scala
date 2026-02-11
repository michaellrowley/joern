package io.joern.adasrc2cpg

import io.joern.adasrc2cpg.Frontend.cmdLineParser
import io.joern.x2cpg.X2CpgMain
import io.joern.x2cpg.passes.frontend.XTypeRecoveryConfig
import scopt.OParser

import java.nio.file.Paths

private object Frontend {
  val cmdLineParser: OParser[Unit, Config] = {
    val builder = OParser.builder[Config]
    import builder._
    OParser.sequence(
      programName("adasrc2cpg"),
      opt[String]("libadalang-path")
        .text("Path to libadalang Python installation. If not specified, uses system default.")
        .action((path, config) => config.withLibadalangPath(Paths.get(path))),
      opt[String]("project-file")
        .text("Path to Ada project file (.gpr) for build configuration.")
        .action((path, config) => config.withProjectFile(Paths.get(path))),
      opt[Map[String, String]]("scenario-variables")
        .text("Scenario variables for project file (format: VAR1=value1,VAR2=value2).")
        .action((vars, config) => config.withScenarioVariables(vars)),
      XTypeRecoveryConfig.parserOptionsForParserConfig
    )
  }
}

object Main extends X2CpgMain(new AdaSrc2Cpg(), cmdLineParser) {
  def main(args: Array[String]): Unit = {
    run(args)
  }
}
