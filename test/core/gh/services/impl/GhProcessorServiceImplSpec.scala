package core.gh.services.impl

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import core.gh.models.response.{FoundRepositories, RepositoriesResponse}
import core.gh.services.github.GitHubServiceImpl
import core.gh.utils.Constants.GhTokenConfig.GH_WS_BASE_URL
import core.gh.utils.GhSpecData._
import core.gh.utils.WSClientMockUtils
import mockws.MockWSHelpers.shutdownHelpers
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Application, Configuration, inject}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.Helpers.stubControllerComponents

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext

class GhProcessorServiceImplSpec
    extends TestKit(ActorSystem("application"))
    with WordSpecLike
    with MustMatchers
    with ScalaFutures
    with MockitoSugar
    with GuiceOneAppPerSuite
    with BeforeAndAfterAll
    with WSClientMockUtils {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val conf                          = Configuration(ConfigFactory.load())

  val ghWsBaseUrl: String = conf.get[String](GH_WS_BASE_URL)

  val wsClient = wsClientMock(ghWsBaseUrl)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(inject.bind[WSClient].toInstance(wsClient))
      .build()

  val ghProcessorServiceImpl =
    new GhProcessorServiceImpl(
      new GitHubApiCalls(conf, new GitHubServiceImpl(conf, wsClient, stubControllerComponents()))
    )

  "hasRateLimit() method" should {
    "return FoundRateLimit when trying get the rate limit remaning for the current configured GH_TOKEN" in {
      whenReady(
        ghProcessorServiceImpl.hasRateLimit()
      ) { _ must be(foundRateLimit) }
    }
  }

  "getAllRepositories() method" should {
    val organization = "owner"
    "return FoundRepositories when trying get 1 page of repositories from a valid organization with rate limit remaning" in {
      whenReady(
        ghProcessorServiceImpl.getAllRepositories(
          organization
        )
      ) { _ must be(FoundRepositories(RepositoriesResponse(Some(Seq(repository2, repository3))))) }
    }

    "return FoundRepositories when trying get repositories from a valid organization with rate limit remaning" in {
      whenReady(
        ghProcessorServiceImpl.getAllRepositories(
          s"${organization}1"
        )
      ) { _ must be(foundRepositories) }
    }

    "return FoundRepositories when trying get repositories from a valid organization with rate limit remaning and Retry-After header in the response" in {
      whenReady(
        ghProcessorServiceImpl.getAllRepositories(
          s"${organization}2"
        ),
        Timeout(5.seconds)
      ) { _ must be(foundRepositories) }
    }
  }

  "getAllContributorsForRepository() method" should {
    "return FoundContributors when trying get contributors from a valid organization with rate limit remaning" in {
      whenReady(
        ghProcessorServiceImpl.getAllContributorsForRepository(
          Seq(repository1, repository2, repository3)
        )
      ) { _ must be(foundContributorsProcessed) }
    }

    "return FoundContributors when trying get contributors from a valid organization with rate limit remaning and Retry-After header in the response" in {
      whenReady(
        ghProcessorServiceImpl.getAllContributorsForRepository(Seq(repository4)),
        Timeout(5.seconds)
      ) { _ must be(foundContributorsWithDelay) }
    }

    "deleteRepeatedContributors() method" should {
      "return Seq[Contributor] when trying delete the repeated contributions and acumulating" in {
        ghProcessorServiceImpl
          .deleteRepeatedContributors(repeatedContributors)
          .sortBy(_.contributions) must be(uniqueContributorsByName)
      }
    }
  }

  override def afterAll(): Unit = {
    shutdownHelpers()
  }
}
