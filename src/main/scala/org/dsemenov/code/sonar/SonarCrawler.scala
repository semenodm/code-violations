package org.dsemenov.code.sonar

import java.io.{BufferedReader, File, FileReader}
import java.lang.Iterable
import java.nio.charset.Charset
import java.util

import org.sonar.java.AnalyzerMessage.TextSpan
import org.sonar.java.ast.JavaAstScanner
import org.sonar.java.model.VisitorsBridgeForTests.TestJavaFileScannerContext
import org.sonar.java.model.{JavaVersionImpl, VisitorsBridge}
import org.sonar.java.resolve.SemanticModel
import org.sonar.java.{AnalyzerMessage, JavaConfiguration, SonarComponents}
import org.sonar.plugins.java.api.tree.{CompilationUnitTree, Tree}
import org.sonar.plugins.java.api.{JavaCheck, JavaFileScanner, JavaFileScannerContext, JavaVersion}

/**
  * Created by dsemenov
  * Date: 3/30/16.
  */
object SonarCrawler {


  case class ReportIssue(check: JavaCheck,
                         file: File,
                         textSpan: AnalyzerMessage.TextSpan,
                         message: String,
                         cost: Int,
                         tree: Tree) extends AnalyzerMessage(check, file, textSpan, message, cost)

  class ScannerContext(tree: CompilationUnitTree, file: File, semanticModel: SemanticModel, analyseAccessors: Boolean, sonarComponents: SonarComponents, javaVersion: JavaVersion, failedParsing: Boolean) extends TestJavaFileScannerContext(tree, file, semanticModel, analyseAccessors, sonarComponents, javaVersion, failedParsing) {
    var issues: List[ReportIssue] = List()

    override def reportIssue(javaCheck: JavaCheck, syntaxNode: Tree, message: String, secondary: util.List[JavaFileScannerContext.Location], cost: Integer) {
      import scala.collection.JavaConversions._
      val file: File = getFile
      val analyzerMessage = new ReportIssue(check = javaCheck,
        file = file,
        textSpan = AnalyzerMessage.textSpanFor(syntaxNode),
        message = message,
        cost = if (cost != null) cost.intValue() else 0,
        tree = syntaxNode)
      for (location <- secondary) {
        val secondaryLocation = new ReportIssue(
          javaCheck,
          file,
          AnalyzerMessage.textSpanFor(location.syntaxNode),
          location.msg, 0,
          location.syntaxNode
        )
        analyzerMessage.secondaryLocations.add(secondaryLocation)
      }
      issues = analyzerMessage :: issues
    }
  }

  class SonarVisitorBridge(scanners: Iterable[JavaFileScanner], projectRoot: util.List[File]) extends VisitorsBridge(scanners, projectRoot, null) {
    var context: ScannerContext = null

    override protected def createScannerContext(tree: CompilationUnitTree, semanticModel: SemanticModel, analyseAccessors: Boolean, sonarComponents: SonarComponents, failedParsing: Boolean): JavaFileScannerContext = {
      context = new ScannerContext(tree, currentFile, semanticModel, analyseAccessors, sonarComponents, javaVersion, failedParsing)
      context
    }
  }

  def extract(file: String, scanners: List[JavaFileScanner]) = {
    import scala.collection.JavaConversions._
    val bridge = new SonarVisitorBridge(scanners, Nil)
    val conf = new JavaConfiguration(Charset.forName("UTF-8"))
    conf.setJavaVersion(JavaVersionImpl.fromString("1.6"))
    JavaAstScanner.scanSingleFileForTests(new File(file), bridge, conf)
    bridge.context.issues

  }

  def substitute(content: String, replacements: List[(TextSpan, String, String)]): String = {
    val ordered: List[(TextSpan, String, String)] = replacements.sortBy { case (location, _, _) => location.startCharacter }
    val prefix = ordered
      .foldLeft((0, "")) { (result, schema) =>
        schema match {
          case (span, _, to) =>
            span.endCharacter -> s"${result._2}${content.substring(result._1, span.startCharacter)}$to"
        }
      }._2
    val postfix = content.substring(ordered.last._1.endCharacter)
    s"$prefix$postfix"
  }

  def extractEOLCharacter(file: File): String = {
    val br = new BufferedReader(new FileReader(file))

    val trailingChars = Stream.continually(br.read().asInstanceOf[Char]).takeWhile { c => c != '\n' && c != -1.toChar }.last
    br.close()
    trailingChars match {
      case '\r' => "\r\n"
      case _ => "\n"
    }
  }
}
