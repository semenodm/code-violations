package org.dsemenov.code.violations

import org.dsemenov.code.violations.checker.ClassVisitor
import org.scalatest.FunSuite
import org.sonar.java.checks.MagicNumberCheck

import scala.io.Source

/**
  * Created by dsemenov
  * Date: 3/30/16.
  */
class MagicNumbersExtractorTest extends FunSuite {

  import org.dsemenov.code.sonar.SonarCrawler._

  test("fix magic numbers in code") {
    val t = new MagicNumberCheck with MagicNumbersExtractor with ClassVisitor
    val filename: String = "src/test/resources/JavaFileWithMagicViolations.java"
    val violations = extract(filename, List(t))
    val result = t.fixMagicNumbers(violations)
    val expected: String = Source.fromFile("src/test/resources/JavaFileWithMagicViolationsFixed.java").mkString
    assert(result === Some(expected))
  }
}
