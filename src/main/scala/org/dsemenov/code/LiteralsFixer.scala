package org.dsemenov.code

import java.io.File

import org.dsemenov.code.Things.{ContentType, Language, ProjectFile}
import org.dsemenov.code.violations.LiteralsViolationExtractor
import org.dsemenov.code.violations.checker.DupeLiteralChecker
import org.sonar.java.AnalyzerMessage
import org.sonar.java.AnalyzerMessage.TextSpan

import scala.collection.JavaConversions._
import scala.io.Source

/**
  * Created by dsemenov
  * Date: 3/28/16.
  */
object LiteralsFixer extends App with LiteralsViolationExtractor {

  CodeNavigator.recoursiveListFiles(new File(args(0)), ContentType.PROJECT)
    .filter { file => file.contentType == ContentType.TEST && file.lang == Language.JAVA }
    .map { file =>
      println(file.file.getName)
      file -> extract(file.file.getAbsolutePath)
    }.foreach { case (file, issues) =>
    fixLiteralIssues(file, issues)
  }

  def fixLiteralIssues(file: ProjectFile, issues: List[AnalyzerMessage]): Unit = {
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
      val res = (source.take(line).toList ::: constants ::: source.takeRight(source.length - line).toList).mkString("\n")
      println(res)

    }
  }

  def buildConstantDeclaration(source: Array[String], issue: AnalyzerMessage): (String, String, String) = {
    val startLine = issue.primaryLocation().startLine
    val start = issue.primaryLocation().startCharacter
    val end = issue.primaryLocation().endCharacter

    val literal = source(startLine - 1).substring(start, end)
    val literalConstant: String = getSuggestionsByValue(literal).head
    val constantDeclaration = s"public static String $literalConstant = $literal;"
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
    result
  }

}
