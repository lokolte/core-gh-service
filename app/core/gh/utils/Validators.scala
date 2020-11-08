package core.gh.utils

import play.api.libs.json.{
  JsArray,
  JsError,
  JsPath,
  JsResult,
  JsSuccess,
  JsValue,
  JsonValidationError,
  Reads
}
import Constants.ErrorMessages.{INVALID_VALUE, REQUIRED_FIELD}

object Validators {
  private def error(msg: String)        = JsonValidationError(msg)
  val DEFAULT_VALIDATOR: Any => Boolean = _ => true

  def isValidName(name: String): Boolean = name.nonEmpty

  def string(path: JsPath,
             key: String,
             errMsg: String = INVALID_VALUE,
             validator: String => Boolean = DEFAULT_VALIDATOR,
             transformer: String => String = _.trim): Reads[String] =
    (path \ key)
      .read[String]
      .map(transformer)
      .filter(error(REQUIRED_FIELD))(_.length > 0)
      .filter(error(errMsg))(validator)

  def int(jsPath: JsPath,
          key: String,
          errMsg: String = INVALID_VALUE,
          validator: Int => Boolean = DEFAULT_VALIDATOR): Reads[Int] =
    (jsPath \ key).read[Int].filter(error(errMsg))(validator)
}
