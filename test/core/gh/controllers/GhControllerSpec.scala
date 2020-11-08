package core.gh.controllers

import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.Injecting
import play.api.cache.Cached

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import TestUtils._
import akka.stream.Materializer
import core.gh.models.response.ServiceResponse
import core.gh.services.GhService
import core.gh.utils.GhSpecData._
import play.api.inject.guice.GuiceApplicationBuilder

class GhControllerSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting with MockitoSugar {

  def mockApp                           = new GuiceApplicationBuilder().build()
  implicit val materializer             = mockApp.injector.instanceOf[Materializer]
  implicit val messagesAPI: MessagesApi = stubMessagesApi()
  val mockGhService: GhService          = mock[GhService]
  val cached: Cached                    = mockApp.injector.instanceOf[Cached]
  val controller =
    new GhController(stubControllerComponents(), mockGhService, cached, stubLangs())

  "GhController GET getGhContributors()" should {

    "return 200 when retrieving all the contributors from a organization given its name" in {
      val organization = "someorg"
      val fakeReq =
        fakeRequestWithoutBody(GET, s"/org/$organization/contributors")
      val assertion =
        (result: Future[Result]) => contentAsJson(result) mustBe Json.toJson(contributorsResponse)

      testStatusAndResponse(
        () => mockGhService.findContributors(organization),
        foundContributors,
        assertion,
        () => controller.getGhContributors(organization)(fakeReq).run(),
        OK
      )
    }

    "return 403 when retrieving contributors by an organization name with no suficient rate limit remaining" in {
      val organization = "anotherorganization" // this to avoid cache
      val fakeReq =
        fakeRequestWithoutBody(GET, s"/org/$organization/contributors")
      testStatus(
        () => mockGhService.findContributors(organization),
        errorMessage2,
        () => controller.getGhContributors(organization)(fakeReq).run(),
        FORBIDDEN
      )
    }

    "return 404 when retrieving contributors by an non-existent organization name" in {
      val organization = "uknownorg"
      val fakeReq =
        fakeRequestWithoutBody(GET, s"/org/$organization/contributors")
      testStatus(
        () => mockGhService.findContributors(organization),
        notFound,
        () => controller.getGhContributors(organization)(fakeReq).run(),
        NOT_FOUND
      )
    }

    "return 500 when retrieving contributors by random network error or currepted data" in {
      val organization = "uknownorg"
      val fakeReq =
        fakeRequestWithoutBody(GET, s"/org/$organization/contributors")
      testStatus(
        () => mockGhService.findContributors(organization),
        serviceError,
        () => controller.getGhContributors(organization)(fakeReq).run(),
        INTERNAL_SERVER_ERROR
      )
    }
  }

  "GhController GET getGhRepositories()" should {

    val organizationName = "someorg"

    "return 200 when retrieving all the repositories from a organization given its name" in {
      val fakeReq =
        fakeRequestWithoutBody(GET, s"/org/$organizationName/repositories")
      val assertion =
        (result: Future[Result]) => contentAsJson(result) mustBe Json.toJson(repositoriesResponse)

      testStatusAndResponse(
        () => mockGhService.findRepository(organizationName),
        foundRepositories,
        assertion,
        () => controller.getGhRepositories(organizationName)(fakeReq),
        OK
      )
    }

    "return 403 when retrieving repositories by an organization name with no suficient rate limit remaining" in {
      val fakeReq =
        fakeRequestWithoutBody(GET, s"/org/$organizationName/repositories")

      testStatus(
        () => mockGhService.findRepository(organizationName),
        errorMessage2,
        () => controller.getGhRepositories(organizationName)(fakeReq),
        FORBIDDEN
      )
    }

    "return 404 when retrieving repositories by an non-existent organization name" in {
      val fakeReq =
        fakeRequestWithoutBody(GET, s"/org/uknownorg/repositories")

      testStatus(
        () => mockGhService.findRepository("uknownorg"),
        notFound,
        () => controller.getGhRepositories("uknownorg")(fakeReq),
        NOT_FOUND
      )
    }
  }

  def testStatus(mockBlockService: () => Any,
                 respService: ServiceResponse,
                 call: () => Future[Result],
                 code: Int = NOT_FOUND): Assertion = {
    when(mockBlockService()).thenReturn(Future(respService))
    status(call()) mustBe code
  }

  def testStatusAndResponse(mockBlockService: () => Any,
                            respService: ServiceResponse,
                            assertion: Future[Result] => Assertion,
                            call: () => Future[Result],
                            code: Int = NOT_FOUND): Assertion = {
    when(mockBlockService()).thenReturn(Future(respService))
    val result = call()
    status(result) mustBe code
    assertion(result)
  }

}
