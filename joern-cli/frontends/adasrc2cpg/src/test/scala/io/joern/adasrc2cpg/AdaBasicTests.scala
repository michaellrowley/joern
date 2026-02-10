package io.joern.adasrc2cpg

import io.joern.x2cpg.testfixtures.{Code2CpgFixture, LanguageFrontend}
import io.shiftleft.semanticcpg.language._
import org.scalatest.Inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AdaSrc2CpgTestFixture extends Code2CpgFixture(() => new AdaSrc2Cpg()) {
  override val fileSuffix: String = ".adb"
}

class AdaBasicTests extends AnyWordSpec with Matchers with Inside {

  "AdaSrc2Cpg" should {
    "create a CPG" in AdaSrc2CpgTestFixture { fixture =>
      val cpg = fixture.cpg
      cpg.method.name.l should not be empty
    }
  }
}
