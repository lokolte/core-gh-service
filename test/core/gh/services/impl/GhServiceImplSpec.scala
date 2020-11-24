package core.gh.services.impl

import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import core.gh.models.response.{ErrorMessage, FoundContributors, FoundRepositories, NotFound}
import core.gh.utils.Constants.ErrorMessages.{
  RATE_LIMIT_REMAINING_ERROR,
  RATE_LIMIT_REMAINING_ERROR_REST,
  RESOURCE_NOTFOUND
}
import core.gh.utils.GhSpecData._

import scala.concurrent.{ExecutionContext, Future}

class GhServiceImplSpec extends WordSpec with MustMatchers with ScalaFutures with MockitoSugar {

  implicit val ec: ExecutionContext                      = ExecutionContext.Implicits.global
  val mockGhProcessorServiceImpl: GhProcessorServiceImpl = mock[GhProcessorServiceImpl]
  val ghServiceImpl                                      = new GhServiceImpl(mockGhProcessorServiceImpl)

  "findRepository() method" should {
    val organization = "organization"
    "return NotFound when trying get repositories from an organization that does not exists" in {
      when(mockGhProcessorServiceImpl.hasRateLimit()).thenReturn(Future(foundRateLimit))
      when(mockGhProcessorServiceImpl.getAllRepositories(organization)).thenReturn(Future(notFound))
      whenReady(
        ghServiceImpl.findRepository(organization)
      ) { _ must be(NotFound(RESOURCE_NOTFOUND)) }
    }

    "return ErrorMessage when trying get repositories when doesn't have rate limit remaining left in the middle of the request" in {
      when(mockGhProcessorServiceImpl.hasRateLimit()).thenReturn(Future(foundRateLimit))
      when(mockGhProcessorServiceImpl.getAllRepositories(organization))
        .thenReturn(Future(errorMessage2))
      whenReady(
        ghServiceImpl.findRepository(organization)
      ) { _ must be(ErrorMessage(RATE_LIMIT_REMAINING_ERROR_REST)) }
    }

    "return ErrorMessage when trying get repositories when doesn't have rate limit remaining left at the beggining" in {
      when(mockGhProcessorServiceImpl.hasRateLimit()).thenReturn(Future(errorMessage1))
      whenReady(
        ghServiceImpl.findRepository(organization)
      ) { _ must be(ErrorMessage(RATE_LIMIT_REMAINING_ERROR)) }
    }

    "return FoundRepositories when trying get repositories from a valid organization with rate limit remaning" in {
      when(mockGhProcessorServiceImpl.hasRateLimit()).thenReturn(Future(foundRateLimit))
      when(mockGhProcessorServiceImpl.getAllRepositories(organization))
        .thenReturn(Future(foundRepositories))
      whenReady(
        ghServiceImpl.findRepository(organization)
      ) { _ must be(FoundRepositories(repositoriesResponse)) }
    }
  }

  "findContributors() method" should {
    val organization = "organization"
    "return NotFound when trying get contributors from an organization that does not exists" in {
      when(mockGhProcessorServiceImpl.hasRateLimit()).thenReturn(Future(foundRateLimit))
      when(
        mockGhProcessorServiceImpl
          .getAllContributorsForRepository(Seq(repository1, repository2, repository3))
      ).thenReturn(Future(notFound))
      whenReady(
        ghServiceImpl.findContributors(organization)
      ) { _ must be(NotFound(RESOURCE_NOTFOUND)) }
    }

    "return ErrorMessage when trying get contributors when doesn't have rate limit remaining left in the middle of the request" in {
      when(mockGhProcessorServiceImpl.hasRateLimit()).thenReturn(Future(foundRateLimit))
      when(
        mockGhProcessorServiceImpl
          .getAllContributorsForRepository(Seq(repository1, repository2, repository3))
      ).thenReturn(Future(errorMessage2))
      whenReady(
        ghServiceImpl.findContributors(organization)
      ) { _ must be(ErrorMessage(RATE_LIMIT_REMAINING_ERROR_REST)) }
    }

    "return ErrorMessage when trying get contributors when doesn't have rate limit remaining left at the beggining" in {
      when(mockGhProcessorServiceImpl.hasRateLimit()).thenReturn(Future(errorMessage1))
      whenReady(
        ghServiceImpl.findContributors(organization)
      ) { _ must be(ErrorMessage(RATE_LIMIT_REMAINING_ERROR)) }
    }

    "return FoundContributors when trying get contributors from a valid organization with rate limit remaning" in {
      when(mockGhProcessorServiceImpl.hasRateLimit()).thenReturn(Future(foundRateLimit))
      when(mockGhProcessorServiceImpl.getAllRepositories(organization))
        .thenReturn(Future(foundRepositories))
      when(
        mockGhProcessorServiceImpl
          .getAllContributorsForRepository(any(), any(), any())
      ).thenReturn(Future(foundContributors))

      when(
        mockGhProcessorServiceImpl
          .deleteRepeatedContributors(any())
      ).thenReturn(Seq(contributor1, contributor2, contributor3))

      whenReady(
        ghServiceImpl.findContributors(organization)
      ) { result => result must be(FoundContributors(contributorsResponse)) }
    }
  }
}
