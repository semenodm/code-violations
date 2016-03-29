package org.dsemenov.code.violations

import org.scalatest.FunSuite
import org.sonar.java.AnalyzerMessage.TextSpan

/**
  * Created by dsemenov
  * Date: 3/28/16.
  */
class LiteralsViolationExtractorTest extends FunSuite with LiteralsViolationExtractor {

  test("substitute line with literals") {
    assert(substitute("literals.add literal1 literals.add literal2 bla lab literal2 lab",
      List((new TextSpan(0, 13, 0, 21), "literal1", "NEW_LITERAL_1"),
        (new TextSpan(0, 35, 0, 43), "literal2", "NEW_LITERAL_2"),
        (new TextSpan(0, 52, 0, 60), "literal2", "NEW_LITERAL_2")
      )
    ) === "literals.add NEW_LITERAL_1 literals.add NEW_LITERAL_2 bla lab NEW_LITERAL_2 lab")
  }

  test("substitute line with one literal") {
    assert(substitute("literals.add literal1 ",
      List((new TextSpan(0, 13, 0, 21), "literal1", "NEW_LITERAL_1"))
    ) === "literals.add NEW_LITERAL_1 ")
  }

  test("literal at the beginning of the string") {
    assert(substitute("literal1 literals.add ",
      List((new TextSpan(0, 0, 0, 8), "literal1", "NEW_LITERAL_1"))
    ) === "NEW_LITERAL_1 literals.add ")
  }

  test("literals on by one ") {
    assert(substitute("literal1literal1 literals.add ",
      List((new TextSpan(0, 0, 0, 8), "literal1", "NEW_LITERAL_1"),
        (new TextSpan(0, 8, 0, 16), "literal1", "NEW_LITERAL_1"))
    ) === "NEW_LITERAL_1NEW_LITERAL_1 literals.add ")
  }
  test("from class") {
    assert(substitute("    literals.add(\"literal1\");",
      List((new TextSpan(0, 17, 0, 27), "literal1", "NEW_LITERAL_1"))
      ) === "    literals.add(NEW_LITERAL_1);")
  }

  test("from class 2") {
    assert(substitute("    literals.add(\"literal1\"); literals.add(\"literal2\");",
      List((new TextSpan(0, 43, 0, 53), "literal2", "LITERAL2"),
        (new TextSpan(0, 17, 0, 27), "literal1", "LITERAL1")
      )
    ) === "    literals.add(LITERAL1); literals.add(LITERAL2);")
  }

  test("tool suggest appropriate constant name"){
    assert(getSuggestionsByValue("UTF-8") === "UTF_8")
  }
  test("tool suggest appropriate constant name for whitespaces"){
    assert(getSuggestionsByValue("     ") === "EMPTY")
  }
}
