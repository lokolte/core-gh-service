package core.gh.utils

import org.scalatest.{Assertion, MustMatchers}
import play.api.libs.json._

import scala.collection.Seq

trait ValidationUtils extends MustMatchers {

  def validateJsError[T](result: JsResult[T], key: String, errMsg: String): Option[Assertion] = {
    val allErrors = result.asEither.left.toOption
    allErrors.map { errors =>
      val err: (JsPath, Seq[JsonValidationError]) = errors.head
      err._1.path.head.toString.substring(1) must be(key)
      val validationErrors = err._2
      validationErrors.size must be(1)
      validationErrors.head.message must be(errMsg)
    }
  }
}
