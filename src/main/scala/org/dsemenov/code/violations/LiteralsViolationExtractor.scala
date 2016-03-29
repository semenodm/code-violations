package org.dsemenov.code.violations

import java.io.{File, FileWriter, PrintWriter}
import java.nio.charset.Charset

import org.dsemenov.code.Things.ProjectFile
import org.dsemenov.code.violations.checker.DupeLiteralChecker
import org.sonar.java.AnalyzerMessage.TextSpan
import org.sonar.java.ast.JavaAstScanner
import org.sonar.java.model.{JavaVersionImpl, VisitorsBridgeForTests}
import org.sonar.java.{AnalyzerMessage, JavaConfiguration}

import scala.collection.JavaConversions._
import scala.io.Source

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

  def constantValueToConstantName(names : List[String]) : String =  {
    val constant: String = names.mkString("_")
    if(constant.trim.isEmpty) "EMPTY" else constant
  }
}
