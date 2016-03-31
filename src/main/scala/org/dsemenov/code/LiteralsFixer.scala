package org.dsemenov.code

import java.io.File

import org.dsemenov.code.Things.{ContentType, Language}
import org.dsemenov.code.violations.LiteralsViolationExtractor
import org.dsemenov.code.violations.checker.DupeLiteralChecker

/**
  * Created by dsemenov
  * Date: 3/28/16.
  */
object LiteralsFixer extends App with LiteralsViolationExtractor {

  import org.dsemenov.code.sonar.SonarCrawler._

  CodeNavigator.recoursiveListFiles(new File(args(0)), ContentType.PROJECT)
    .filter { file => file.contentType == ContentType.TEST && file.lang == Language.JAVA }
    .map { file =>
      println(file.file.getName)
      file -> extract(file.file.getAbsolutePath, List(new DupeLiteralChecker))
    }.foreach { case (file, issues) =>
    fixLiteralIssues(file, issues)
  }
}
