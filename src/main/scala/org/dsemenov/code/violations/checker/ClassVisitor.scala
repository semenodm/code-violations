package org.dsemenov.code.violations.checker

import org.sonar.plugins.java.api.JavaFileScanner
import org.sonar.plugins.java.api.tree.{BaseTreeVisitor, ClassTree}

/**
  * Created by dsemenov
  * Date: 3/30/16.
  */
trait ClassVisitor extends BaseTreeVisitor with JavaFileScanner {
  var parentClass: Option[ClassTree] = None

  //  override def scanFile(context: JavaFileScannerContext): Unit = {
  //    super.scan(context.getTree)
  //  }

  override def visitClass(tree: ClassTree): Unit = {
    parentClass = parentClass.orElse(Some(tree))
    super.visitClass(tree)
  }

}
