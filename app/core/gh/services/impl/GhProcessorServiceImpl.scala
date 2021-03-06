package core.gh.services.impl

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import core.gh.models.response.headers.LinkPages
import core.gh.models.{Contributor, Repository}
import core.gh.models.response.{
  ContributorsResponse,
  ErrorMessage,
  FoundContributors,
  FoundRateLimit,
  FoundRepositories,
  NoContent,
  NotFound,
  RepositoriesResponse,
  ServiceError,
  ServiceResponse
}
import core.gh.services.github.GitHubApiCalls
import core.gh.utils.Constants.ErrorMessages.RATE_LIMIT_REMAINING_ERROR
import javax.inject.{Inject, Singleton}
import play.api.Logging

import scala.concurrent.{ExecutionContext, Future, Promise, duration}
import scala.collection.mutable

@Singleton
class GhProcessorServiceImpl @Inject() (gitHubApiCalls: GitHubApiCalls)(
    implicit val system: ActorSystem,
    ec: ExecutionContext
) extends Logging {

  def hasRateLimit(): Future[ServiceResponse] = {
    gitHubApiCalls.hasRateLimit().map {
      case serviceError: ServiceError => serviceError
      case foundRateLimit: FoundRateLimit =>
        if (foundRateLimit.rateLimitResponse.rateLimit.get.remaining > 0)
          foundRateLimit
        else ErrorMessage(RATE_LIMIT_REMAINING_ERROR)
    }
  }

  def getAllRepositories(organization: String): Future[ServiceResponse] = {
    logger.info(s"Repositories from: org=[$organization]")
    getRepositoriesByPages(
      organization
    )
  }

  /**
    * This function get all the repositories from an organization recursively using the current page, calling to GitHub API and waits for the result
    *    if the result of this call is a `ServiceError` then finish,
    *    otherwise, look at the response,
    *    - if the response has a body with data, will merge it with the acumulator,
    *    - if the response have the header `link` page will continue to hit to the API with the next page number,
    *    - if the body contains data with, a smaller or equal to zero, number of elements than the page, then finish,
    *    - if the response comes with the Header `Retry-After` will wait whit the number of seconds specified.
    * @param organization organization of the repositories
    * @param page the current page
    * @param foundRepositoriesAcumulator acumulator of repositories to merge with the result
    * @return `Future[ServiceResponse]` can be a `ServiceError` or `FoundRepositories`
    */
  private def getRepositoriesByPages(
      organization: String,
      page: Int = 0,
      foundRepositoriesAcumulator: FoundRepositories = createFoundRepository(Seq())
  ): Future[ServiceResponse] = {
    gitHubApiCalls.getRepositories(organization, page).flatMap {
      case NoContent() => Future.successful(foundRepositoriesAcumulator)
      case foundRepositories: FoundRepositories =>
        foundRepositories.linkPages match {
          case None => // the first page is the only one
            Future.successful(
              createFoundRepository(
                foundRepositories.repositoryResponse.repositories.get ++ foundRepositoriesAcumulator.repositoryResponse.repositories
                  .getOrElse(Seq())
              )
            )
          case Some(linkPages) =>
            linkPages.next match {
              case None => // this is the last page because there is not next
                Future.successful(
                  createFoundRepository(
                    foundRepositories.repositoryResponse.repositories.get ++ foundRepositoriesAcumulator.repositoryResponse.repositories
                      .getOrElse(Seq())
                  )
                )
              case Some(nextPage) => // move to the next page
                if (foundRepositories.retryAfter == 0) // move witout delay
                  getRepositoriesByPages(
                    organization,
                    nextPage,
                    createFoundRepository(
                      foundRepositories.repositoryResponse.repositories.get ++ foundRepositoriesAcumulator.repositoryResponse.repositories
                        .getOrElse(Seq())
                    )
                  )
                else { // move with delay
                  val promise = Promise[ServiceResponse]
                  system.scheduler.scheduleOnce(delay =
                    duration.Duration(foundRepositories.retryAfter, TimeUnit.SECONDS)
                  ) {
                    promise.completeWith(
                      getRepositoriesByPages(
                        organization,
                        nextPage,
                        createFoundRepository(
                          foundRepositories.repositoryResponse.repositories.get ++ foundRepositoriesAcumulator.repositoryResponse.repositories
                            .getOrElse(Seq())
                        )
                      )
                    )
                  }
                  promise.future
                }
            }
        }
      case other => Future.successful(other)
    }
  }

  private def createFoundRepository(repositories: Seq[Repository],
                                    retryAfter: Int = 0,
                                    linkPages: Option[LinkPages] = None): FoundRepositories = {
    FoundRepositories(
      RepositoriesResponse(
        Some(repositories)
      ),
      retryAfter,
      linkPages
    )
  }

  /**
    * This function will get the contributors of all the repositories of a organization, iterating repositories based on the index,
    * then will merge the result with the acumulator  `FoundContributors`.
    * @param repositories all the repositories of a given organization
    * @param index index of the current repository
    * @param foundContributorsAcumulator acumulator of found contributors to merge with the result
    * @return `Future[ServiceResponse]` can be a `ServiceError` or `FoundRepositories`
    */
  def getAllContributorsForRepository(
      repositories: Seq[Repository],
      index: Int = 0,
      foundContributorsAcumulator: FoundContributors = createFoundContributor(Seq())
  ): Future[ServiceResponse] = {
    if (repositories.nonEmpty && index < repositories.size) {
      logger.info(
        s"Contributors from: repository=[${repositories(index).name}] for org=[${repositories(index).owner.login}]"
      )
      getContributorsByPages(
        repositories(index).owner.login,
        repositories(index).name,
        foundContributorsAcumulator = createFoundContributor(Seq())
      ).flatMap {
        case NoContent() => Future.successful(foundContributorsAcumulator)
        case foundContributors: FoundContributors =>
          getAllContributorsForRepository(
            repositories,
            index + 1,
            createFoundContributor(
              foundContributors.contributorsResponse.contributors
                .getOrElse(Seq()) ++ foundContributorsAcumulator.contributorsResponse.contributors
                .getOrElse(Seq())
            )
          )
        case other => Future.successful(other)
      }
    } else Future.successful(foundContributorsAcumulator)
  }

  /**
    * This function get all the contributors from an organization recursively using the current page, calling to GitHub API and waits for the result
    *    if the result of this call is a `ServiceError` then finish,
    *    otherwise, look at the response,
    *    - if the response has a body with data, will merge it with the acumulator,
    *    - if the response have the header `link` page will continue to hit to the API with the next page number,
    *    - if the body contains data with, a smaller or equal to zero, number of elements than the page, then finish,
    *    - if the response comes with the Header `Retry-After` will wait whit the number of seconds specified.
    * @param organization current organization
    * @param repository current repository for his contributors
    * @param page current page
    * @param foundContributorsAcumulator acumulator of contributors to merge with the result
    * @return `Future[ServiceResponse]` can be a `ServiceError` or `FoundRepositories`
    */
  private def getContributorsByPages(
      organization: String,
      repository: String,
      page: Int = 0,
      foundContributorsAcumulator: FoundContributors
  ): Future[ServiceResponse] = {
    gitHubApiCalls
      .getContributors(
        organization,
        repository,
        page
      )
      .flatMap {
        case NoContent() => Future.successful(foundContributorsAcumulator)
        case foundContributors: FoundContributors =>
          foundContributors.linkPages match {
            case None => // the first page is the only one
              Future.successful(
                createFoundContributor(
                  foundContributors.contributorsResponse.contributors.get ++ foundContributorsAcumulator.contributorsResponse.contributors
                    .getOrElse(Seq())
                )
              )
            case Some(linkPages) =>
              linkPages.next match {
                case None => // this is the last page because there is not next
                  Future.successful(
                    createFoundContributor(
                      foundContributors.contributorsResponse.contributors.get ++ foundContributorsAcumulator.contributorsResponse.contributors
                        .getOrElse(Seq())
                    )
                  )
                case Some(nextPage) => // move to the next page
                  if (foundContributors.retryAfter == 0) // move without delay
                    getContributorsByPages(
                      organization,
                      repository,
                      nextPage,
                      createFoundContributor(
                        foundContributors.contributorsResponse.contributors.get ++ foundContributorsAcumulator.contributorsResponse.contributors
                          .getOrElse(Seq())
                      )
                    )
                  else { // move with delay
                    val promise = Promise[ServiceResponse]()
                    system.scheduler.scheduleOnce(delay =
                      duration.Duration(foundContributors.retryAfter, TimeUnit.SECONDS)
                    ) {
                      promise.completeWith(
                        getContributorsByPages(
                          organization,
                          repository,
                          nextPage,
                          createFoundContributor(
                            foundContributors.contributorsResponse.contributors.get ++ foundContributorsAcumulator.contributorsResponse.contributors
                              .getOrElse(Seq())
                          )
                        )
                      )
                    }
                    promise.future
                  }
              }
          }
        case other => Future.successful(other)
      }
  }

  private def createFoundContributor(contributors: Seq[Contributor],
                                     retryAfter: Int = 0,
                                     linkPages: Option[LinkPages] = None): FoundContributors = {
    FoundContributors(
      ContributorsResponse(
        Some(contributors)
      ),
      retryAfter,
      linkPages
    )
  }

  /**
    * This function delete the repeated contributors and merge them in one record acumulating the contributions in one record.
    * @param contributors sequence of contributors of type `Contributor`
    * @return a new sequence with the total number of contributions by the contributor
    */
  def deleteRepeatedContributors(contributors: Seq[Contributor]): Seq[Contributor] = {
    val distinctsContributors: mutable.HashMap[String, Contributor] = mutable.HashMap()
    contributors.foreach { contributor =>
      if (distinctsContributors.contains(contributor.name))
        distinctsContributors.update(
          contributor.name,
          Contributor(
            contributor.name,
            distinctsContributors(contributor.name).contributions + contributor.contributions
          )
        )
      else distinctsContributors.put(contributor.name, contributor)
    }
    distinctsContributors.toSeq.map(_._2)
  }
}
