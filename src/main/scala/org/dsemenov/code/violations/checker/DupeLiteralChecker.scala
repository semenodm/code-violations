package org.dsemenov.code.violations.checker

import com.google.common.collect.Iterables
import org.sonar.java.model.ModifiersUtils
import org.sonar.plugins.java.api.{JavaFileScanner, JavaFileScannerContext}
import org.sonar.plugins.java.api.tree._

import scala.collection.mutable

/**
  * Created by dsemenov
  * Date: 3/28/16.
  */
class DupeLiteralChecker extends BaseTreeVisitor with JavaFileScanner {
  var parentClass: Option[ClassTree] = None
  val occurrences = new mutable.HashMap[String, mutable.Set[LiteralTree]] with mutable.MultiMap[String, LiteralTree]

  override def scanFile(context: JavaFileScannerContext): Unit = {
    import scala.collection.JavaConversions._
    occurrences.clear()
    scan(context.getTree)
    occurrences filter {
      case (literal, literalTrees) => literalTrees.size >= 3
    } foreach { case (literal, literalTrees) =>
      context.reportIssue(
        this,
        literalTrees.head,
        "Define a constant instead of duplicating this literal ",
        literalTrees.tail.map { literalTree =>
          new JavaFileScannerContext.Location("Duplication", literalTree)
        }.toList,
        literalTrees.size)
    }
  }

  override def visitClass(tree: ClassTree): Unit = {
    parentClass = parentClass.orElse(Some(tree))
    super.visitClass(tree)
  }

  override def visitMethod(tree: MethodTree): Unit = {
    if (ModifiersUtils.hasModifier(tree.modifiers, Modifier.DEFAULT)) {
      return
    }
    super.visitMethod(tree)

  }

  override def visitLiteral(tree: LiteralTree): Unit = {
    if (tree.is(Tree.Kind.STRING_LITERAL)) {
      val literal: String = tree.value
      if (literal.length >= 7) {
        occurrences.addBinding(literal, tree)
      }
    }
  }
}
