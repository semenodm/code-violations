package org.dsemenov.code

import java.io.File

import org.apache.commons.io.FilenameUtils
import org.dsemenov.code.Things.ContentType.ContentType
import org.dsemenov.code.Things.Language.Language

/**
  * Created by dsemenov
  * Date: 3/28/16.
  */
object Things {

  object ContentType extends Enumeration {
    type ContentType = Value
    val MAIN, TEST, BYTECODE, PROJECT = Value
  }

  object Language extends Enumeration {
    type Language = Value
    val JAVA, SCALA, GROOVY, JSP, CLASS, OTHER = Value
    def fromFile(file : File): Language ={
      FilenameUtils.getExtension(file.getName) match {
        case "java" => JAVA
        case "groovy" => GROOVY
        case "scala" => SCALA
        case _ => OTHER
      }
    }
  }

  case class ProjectFile(file : File, lang : Language, contentType: ContentType)

}
