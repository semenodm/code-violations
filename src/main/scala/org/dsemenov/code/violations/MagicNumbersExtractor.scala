package org.dsemenov.code.violations

import java.io.File

import org.dsemenov.code.sonar.SonarCrawler.ReportIssue
import org.dsemenov.code.violations.checker.ClassVisitor
import org.sonar.plugins.java.api.tree.Tree.Kind

import scala.io.Source


/**
  * Created by dsemenov
  * Date: 3/30/16.
  */
trait MagicNumbersExtractor {
  this: ClassVisitor =>
  def fixMagicNumbers(violations: List[ReportIssue]) = {
    import org.dsemenov.code.sonar.SonarCrawler._
    for (
      classMeta <- parentClass;
      firstViolation <- violations.headOption
    ) yield {
      val file: File = firstViolation.getFile
      val source = Source.fromFile(file).getLines().toArray
      val numbers = violations.map { magic =>
        val line = magic.primaryLocation().startLine
        val start = magic.primaryLocation().startCharacter
        val end = magic.primaryLocation().endCharacter
        source(line - 1).substring(start, end)
      }

      val violationsByLine = violations zip numbers groupBy { case (violation, number) => violation.primaryLocation.startLine - 1 }

      violationsByLine.foreach { case (line, violations) =>
        val substitutions = violations.map { case (violation, number) =>
          (violation.primaryLocation(), number, numberToConstant(violation, number))
        }
        source(line) = substitute(source(line), substitutions)
      }



      val constants = extractConstants(numbers zip violations)

      val line = classMeta.openBraceToken().line()
      val res = (source.take(line).toList ::: constants ::: source.takeRight(source.length - line).toList)
        .mkString(extractEOLCharacter(file))
      res
    }
  }

  def numberToConstant(violation: ReportIssue, value: String): String = {
    violation.tree.kind() match {
      case Kind.INT_LITERAL | Kind.LONG_LITERAL => s"_$value"
      case Kind.DOUBLE_LITERAL | Kind.FLOAT_LITERAL => s"_${value.replace('.', '_')}"
    }
  }

  def extractConstants(violations: List[(String, ReportIssue)]) = {
    violations.map { case (value, violation) =>
      val constantType = violation.tree.kind() match {
        case Kind.INT_LITERAL => "int"
        case Kind.LONG_LITERAL => "long"
        case Kind.FLOAT_LITERAL => "float"
        case Kind.DOUBLE_LITERAL => "double"
      }

      s"public static final $constantType ${numberToConstant(violation, value)} = $value;"
    }.distinct.sorted
  }
}

