package org.dsemenov.code

import java.io.{File, FileWriter, PrintWriter}

import org.dsemenov.code.Things.{ContentType, Language}
import org.dsemenov.code.violations.MagicNumbersExtractor
import org.dsemenov.code.violations.checker.ClassVisitor
import org.sonar.java.checks.MagicNumberCheck

/**
  * Created by dsemenov
  * Date: 3/28/16.
  */
object MagicNumberFixer extends App {

  import org.dsemenov.code.sonar.SonarCrawler._

  CodeNavigator.recoursiveListFiles(new File(args(0)), ContentType.PROJECT)
    .filter { file => file.contentType == ContentType.TEST && file.lang == Language.JAVA }
    .map { file =>
      println(file.file.getName)
      val t = new MagicNumberCheck with MagicNumbersExtractor with ClassVisitor
      val violations = extract(file.file.getAbsolutePath, List(t))
      val fixed = t.fixMagicNumbers(violations)
      file -> fixed
    }.foreach { case (file, text) =>
    text.foreach { source =>
      val writer: PrintWriter = new PrintWriter(new FileWriter(file.file))
      writer.write(source)
      writer.flush()
      writer.close()
    }
  }
}
