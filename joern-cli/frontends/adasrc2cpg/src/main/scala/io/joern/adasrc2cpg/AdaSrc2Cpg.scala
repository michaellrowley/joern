package io.joern.adasrc2cpg

import io.joern.adasrc2cpg.passes.{AstCreationPass, LibadalangRunner}
import io.joern.x2cpg.X2Cpg.withNewEmptyCpg
import io.joern.x2cpg.X2CpgFrontend
import io.joern.x2cpg.passes.frontend.MetaDataPass
import io.joern.x2cpg.utils.Report
import io.shiftleft.codepropertygraph.generated.{Cpg, Languages}
import io.shiftleft.semanticcpg.utils.FileUtil

import scala.util.Try

class AdaSrc2Cpg extends X2CpgFrontend {
  override type ConfigType = Config
  override val defaultConfig: Config = Config.default

  private val report: Report = new Report()

  def createCpg(config: Config): Try[Cpg] = {
    withNewEmptyCpg(config.outputPath, config) { (cpg, config) =>
      FileUtil.usingTemporaryDirectory("adasrc2cpgOut") { tmpDir =>
        // Create metadata
        MetaDataPass(cpg, Languages.ADA, config.inputPath).createAndApply()
        
        // Parse Ada files using libadalang
        val astGenResult = new LibadalangRunner(config).execute(tmpDir)
        
        // Create CPG from parsed ASTs
        astGenResult match {
          case Some(parsedFiles) =>
            new AstCreationPass(cpg, parsedFiles, config, report).createAndApply()
          case None =>
            report.addError("Failed to parse Ada files")
        }
      }
      report.print()
    }
  }
}
