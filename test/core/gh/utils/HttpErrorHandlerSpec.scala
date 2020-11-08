package core.gh.utils

import org.scalatest.{MustMatchers, WordSpec}
import play.api.{Environment, OptionalSourceMapper}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._

class HttpErrorHandlerSpec extends WordSpec with MustMatchers {

  val errorHandler = new HttpErrorHandler(
    Environment.simple(),
    new OptionalSourceMapper(None)
  )

  "ErrorHandler" should {
    "404" should {
      val badUri = "/bad-url"
      val resultContent = Helpers.contentAsString(
        errorHandler.onClientError(
          FakeRequest(GET, badUri),
          NOT_FOUND,
          ""
        )
      )

      "should return path uri" in {
        resultContent must include(badUri)
      }
    }

    "400" should {
      val message = "malformed json"
      val resultContent = Helpers.contentAsString(
        errorHandler.onClientError(
          FakeRequest(GET, "/bad-url"),
          BAD_REQUEST,
          message
        )
      )

      "should return message" in {
        resultContent must include(message)
      }
    }

    "4xx" should {
      val message = "unprocessable"
      val resultContent = Helpers.contentAsString(
        errorHandler.onClientError(
          FakeRequest(GET, "/bad-url"),
          UNPROCESSABLE_ENTITY,
          message
        )
      )

      "should return message" in {
        resultContent must include(message)
      }
    }

    "5xx" should {
      val exception = new RuntimeException("message")
      val resultContent = Helpers.contentAsString(
        errorHandler.onServerError(
          FakeRequest(GET, "/bad-url"),
          exception
        )
      )

      "should return message" in {
        resultContent must include(exception.getMessage)
      }
    }
  }
}
