package io.joern.adasrc2cpg

import io.joern.x2cpg.X2CpgConfig
import io.joern.x2cpg.passes.frontend.XTypeRecoveryConfig

import java.nio.file.{Path, Paths}

case class Config(
  libadalangPath: Option[Path] = None,
  projectFile: Option[Path] = None,
  scenarioVariables: Map[String, String] = Map.empty
) extends X2CpgConfig[Config]
    with XTypeRecoveryConfig[Config] {

  def withLibadalangPath(path: Path): Config = {
    copy(libadalangPath = Some(path)).withInheritedFields(this)
  }

  def withProjectFile(path: Path): Config = {
    copy(projectFile = Some(path)).withInheritedFields(this)
  }

  def withScenarioVariables(variables: Map[String, String]): Config = {
    copy(scenarioVariables = variables).withInheritedFields(this)
  }
}

object Config {
  def default: Config = Config()
}
