package core.gh.services.impl

import core.gh.models.response.{
  ContributorsResponse,
  ErrorMessage,
  FoundContributors,
  FoundRepositories,
  NotFound,
  ServiceError,
  ServiceResponse
}
import core.gh.services.GhService
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GhServiceImpl @Inject() (ghProcessorServiceImpl: GhProcessorServiceImpl)(
    implicit ec: ExecutionContext
) extends GhService {

  override def findRepository(organization: String): Future[ServiceResponse] = {
    ghProcessorServiceImpl
      .hasRateLimit()
      .flatMap {
        case serviceError: ServiceError => Future.successful(serviceError)
        case errorMessage: ErrorMessage => Future.successful(errorMessage)
        case _ =>
          ghProcessorServiceImpl
            .getAllRepositories(organization)
      }
  }

  override def findContributors(organization: String): Future[ServiceResponse] = {
    findRepository(organization).flatMap {
      case serviceError: ServiceError => Future.successful(serviceError)
      case notFound: NotFound         => Future.successful(notFound)
      case errorMessage: ErrorMessage => Future.successful(errorMessage)
      case foundRepositories: FoundRepositories =>
        ghProcessorServiceImpl
          .getAllContributorsForRepository(
            repositories = foundRepositories.repositoryResponse.repositories.get
          )
          .map {
            case serviceError: ServiceError => serviceError
            case notFound: NotFound         => notFound
            case errorMessage: ErrorMessage => errorMessage
            case foundContributors: FoundContributors =>
              FoundContributors(
                ContributorsResponse(
                  Some(
                    ghProcessorServiceImpl
                      .deleteRepeatedContributors(
                        foundContributors.contributorsResponse.contributors.get
                      )
                      .sortBy(_.contributions)
                  )
                )
              )
          }
    }
  }
}
