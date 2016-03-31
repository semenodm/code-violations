package org.dsemenov.code.violations

import java.io._

import org.dsemenov.code.Things.ProjectFile
import org.dsemenov.code.sonar.SonarCrawler.ReportIssue
import org.dsemenov.code.violations.checker.DupeLiteralChecker
import org.sonar.java.AnalyzerMessage

import scala.collection.JavaConversions._
import scala.io.Source

/**
  * Created by dsemenov
  * Date: 3/28/16.
  */
trait LiteralsViolationExtractor {

  def fixLiteralIssues(file: ProjectFile, issues: List[ReportIssue]): Unit = {
    import org.dsemenov.code.sonar.SonarCrawler._
    for (
      firstIssue <- issues.headOption;
      classDeclaration <- firstIssue.getCheck.asInstanceOf[DupeLiteralChecker].parentClass
    ) yield {
      val source = Source.fromFile(file.file).getLines().toArray
      val substitutions = issues.flatMap { issue =>
        val (literal: String, literalConstant: String, constantDeclaration: String) = buildConstantDeclaration(source, issue)
        (issue :: issue.secondaryLocations.toList).map { secondaryIssue =>
          (secondaryIssue.primaryLocation, literal, literalConstant)
        }
      } groupBy { case (location, _, _) => location.startLine - 1 }

      val constants = issues.map { issue =>
        val (_, _, constantDeclaration: String) = buildConstantDeclaration(source, issue)
        constantDeclaration
      }

      substitutions map { case (line, replacements) =>
        val content = source(line)
        val substituted = substitute(content, replacements)
        line -> substituted
      } foreach { case (line, replacement) =>
        source(line) = replacement
      }

      val line = classDeclaration.openBraceToken().line()
      val res = (source.take(line).toList ::: constants ::: source.takeRight(source.length - line).toList)
        .mkString(extractEOLCharacter(file.file))
      val writer: PrintWriter = new PrintWriter(new FileWriter(file.file))
      writer.write(res)
      writer.flush()
      writer.close()
    }
  }

  def buildConstantDeclaration(source: Array[String], issue: AnalyzerMessage): (String, String, String) = {
    val startLine = issue.primaryLocation().startLine
    val start = issue.primaryLocation().startCharacter
    val end = issue.primaryLocation().endCharacter

    val literal = source(startLine - 1).substring(start, end)
    val literalConstant: String = getSuggestionsByValue(literal)
    val constantDeclaration = s"public static final String $literalConstant = $literal;"
    (literal, literalConstant, constantDeclaration)
  }

  def getSuggestionsByValue(stringValue: String) = {
    var result = List[String]()
    var currentWord: StringBuffer = new StringBuffer()

    var prevIsUpperCase: Boolean = false
    stringValue.foreach {
      c =>
        if (Character.isUpperCase(c)) {
          if (currentWord.length() > 0 && !prevIsUpperCase) {
            result = currentWord.toString :: result
            currentWord = new StringBuffer()
          }
          currentWord.append(c)
        } else if (Character.isLowerCase(c)) {
          currentWord.append(Character.toUpperCase(c))
        } else if (Character.isJavaIdentifierPart(c) && c != '_') {
          if (Character.isJavaIdentifierStart(c) || currentWord.length() > 0 || result.nonEmpty) {
            currentWord.append(c)
          }
        } else {
          if (currentWord.length() > 0) {
            result = currentWord.toString :: result
            currentWord = new StringBuffer()
          }
        }
        prevIsUpperCase = Character.isUpperCase(c)
    }

    if (currentWord.length() > 0) {
      result = currentWord.toString :: result
    }
    constantValueToConstantName(result.reverse)
  }

  def constantValueToConstantName(names: List[String]): String = {
    val constant: String = names.mkString("_")
    if (constant.trim.isEmpty) "EMPTY" else constant
  }
}
