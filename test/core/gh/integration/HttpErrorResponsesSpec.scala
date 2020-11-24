package core.gh.integration

import core.gh.utils.HttpErrorHandler
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.components.OneServerPerSuiteWithComponents
import play.api._
import play.api.mvc.{ControllerHelpers, Headers, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.api.routing.Router
import core.gh.utils.HttpJsonResponseHelper._

import scala.concurrent.Future

class HttpErrorResponsesSpec extends PlaySpec with OneServerPerSuiteWithComponents {

  override def components: BuiltInComponents =
    new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents with ControllerHelpers {

      import play.api.mvc.Results
      import play.api.routing.sird._

      override lazy val httpErrorHandler: HttpErrorHandler =
        new HttpErrorHandler(environment, new OptionalSourceMapper(None))

      lazy val router: Router = Router.from({
        case POST(p"/bad-request") =>
          defaultActionBuilder(parse.json) { request => Results.Ok(request.body) }
      })
    }

  "HTTP Errors" must {
    "return the expected 400 error message" in {
      val Some(result: Future[Result]) =
        route(
          app,
          FakeRequest(
            method = POST,
            uri = "/bad-request",
            body = "{malformed json}",
            headers = Headers(("Content-Type", "application/json"))
          )
        )

      Helpers.contentAsString(result) must include("Invalid Json:")
    }

    "return the expected 404 error message" in {
      val Some(result: Future[Result]) = route(app, FakeRequest(GET, "/bad-url"))
      Helpers.contentAsJson(result) must be(error("/bad-url"))
    }
  }
}
