package org.dsemenov.code.violations

import java.io.File
import java.nio.charset.Charset

import org.dsemenov.code.violations.checker.DupeLiteralChecker
import org.sonar.java.AnalyzerMessage.TextSpan
import org.sonar.java.JavaConfiguration
import org.sonar.java.ast.JavaAstScanner
import org.sonar.java.model.{JavaVersionImpl, VisitorsBridgeForTests}

/**
  * Created by dsemenov
  * Date: 3/28/16.
  */
trait LiteralsViolationExtractor {
  def extract(file: String) = {
    import scala.collection.JavaConversions._
    val bridge = new VisitorsBridgeForTests(List(new DupeLiteralChecker), Nil, null)
    val conf = new JavaConfiguration(Charset.forName("UTF-8"))
    conf.setJavaVersion(JavaVersionImpl.fromString("1.6"))
    JavaAstScanner.scanSingleFileForTests(new File(file), bridge, conf)
    bridge.lastCreatedTestContext.getIssues.toList
  }

  def substitute(content: String, replacements: List[(TextSpan, String, String)]): String = {
    val ordered: List[(TextSpan, String, String)] = replacements.sortBy { case (location, _, _) => location.startCharacter }
    val prefix = ordered
      .foldLeft((0, "")) { (result, schema) =>
        schema match {
          case (span, from, to) =>
            span.endCharacter -> s"${result._2}${content.substring(result._1, span.startCharacter)}$to"
        }
      }._2
    val postfix = content.substring(ordered.last._1.endCharacter)
    s"$prefix$postfix"
  }

}
