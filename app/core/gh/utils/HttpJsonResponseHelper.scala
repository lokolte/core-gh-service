package core.gh.utils

import core.gh.models.response.{Error, Errors}
import play.api.libs.json.{JsValue, Json}
import Constants.ControllerResults._

object HttpJsonResponseHelper {
  def error(error: Error): JsValue = Json.toJson(Errors(Seq(error)))

  def error(message: String): JsValue = error(Error(message))

  def error(jsVals: Seq[JsValue]): JsValue = Json.obj(ERRORS -> jsVals)
}
