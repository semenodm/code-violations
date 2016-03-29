package org.dsemenov.code

import java.io.File

import org.dsemenov.code.Things.ContentType.ContentType
import org.dsemenov.code.Things.{ContentType, Language, ProjectFile}

/**
  * Created by dsemenov
  * Date: 3/28/16.
  */
object CodeNavigator {
  def recoursiveListFiles(f: File, contentType: ContentType): Stream[ProjectFile] = {
    val localFiles = f.listFiles().toStream
    val res = localFiles.filter(_.isFile)
      .map { file => ProjectFile(file, Language.fromFile(file), contentType) }
    res ++ localFiles.filter(_.isDirectory).flatMap { dir =>
      val newContentType = dir.getName match {
        case "main" => ContentType.MAIN
        case "test" => ContentType.TEST
        case _ => contentType
      }
      recoursiveListFiles(dir, newContentType)
    }
  }
}
