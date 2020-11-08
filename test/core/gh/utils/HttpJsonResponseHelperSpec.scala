package core.gh.utils

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsObject, Json}
import Constants.ControllerResults._
import HttpJsonResponseHelper._

class HttpJsonResponseHelperSpec extends WordSpec with MustMatchers {

  val strMessage1 = "message1"
  val strMessage2 = "message2"

  "BaseController" should {
    "method error()" in {
      error(strMessage1) must be(
        Json.obj(ERRORS -> Json.arr(Json.obj(MESSAGE -> strMessage1)))
      )

      val errorMessages: Seq[JsObject] =
        Seq(Json.obj(MESSAGE -> strMessage1), Json.obj(MESSAGE -> strMessage2))
      error(errorMessages) must be(
        Json.obj(ERRORS -> errorMessages)
      )
    }
  }
}
