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
  NotFound,
  RateLimitResponse,
  RepositoriesResponse,
  ServiceError,
  ServiceResponse
}
import core.gh.services.ServiceUtils
import core.gh.utils.Constants.ErrorMessages.{
  RATE_LIMIT_REMAINING_ERROR,
  RATE_LIMIT_REMAINING_ERROR_REST,
  RESOURCE_NOTFOUND
}
import core.gh.utils.Constants.GhHeadersName.{FIRST, LAST, LINK, NEXT, PREV, RETRY_AFTER}
import core.gh.utils.Constants.GhTokenConfig.{
  GH_TOKEN,
  GH_WS_BASE_URL,
  GH_WS_REGISTER_PER_PAGE,
  GH_WS_TIMEOUT
}
import core.gh.utils.Constants.GhWsConstants.{PAGE_PARAM, REGISTERS_PER_PAGE_PARAM, TOKEN}
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logging}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.libs.json._
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION}
import play.api.http.ContentTypes.JSON
import play.api.http.Status.{FORBIDDEN, NOT_FOUND}
import play.api.mvc.ControllerComponents

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future, Promise, duration}
import scala.language.postfixOps
import play.api.libs.ws.WSResponse

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

@Singleton
class GhProcessorServiceImpl @Inject() (conf: Configuration,
                                        ws: WSClient,
                                        val controllerComponents: ControllerComponents)(
    implicit val system: ActorSystem,
    ec: ExecutionContext
) extends ServiceUtils
    with Logging {

  val timeOut: Int        = conf.get[Int](GH_WS_TIMEOUT)
  val ghToken: String     = conf.get[String](GH_TOKEN)
  val ghWsBaseUrl: String = conf.get[String](GH_WS_BASE_URL)
  val ghWsRegPerPage: Int = conf.get[Int](GH_WS_REGISTER_PER_PAGE)

  private def getFromUrl(url: String,
                         headers: Seq[(String, String)],
                         params: Seq[(String, String)]): Future[WSResponse] = {
    val complexRequest: WSRequest =
      ws.url(url)
        .addHttpHeaders(headers: _*)
        .addQueryStringParameters(params: _*)
        .withRequestTimeout(timeOut seconds)

    complexRequest.get()
  }

  private def getGhApiCall[R](url: String, page: Int = 0, withParams: Boolean = true)(
      implicit reads: Reads[R]
  ): Future[ServiceResponse] = {
    val params =
      if (withParams)
        Seq((PAGE_PARAM -> s"$page"), (REGISTERS_PER_PAGE_PARAM -> s"$ghWsRegPerPage"))
      else Seq()
    val headers = Seq((ACCEPT -> JSON), (AUTHORIZATION -> s"$TOKEN $ghToken"))
    getFromUrl(
      url,
      headers,
      params
    ).map(processJson[R](_))
      .recover(recovery)
  }

  def hasRateLimit(): Future[ServiceResponse] = {
    getGhApiCall[RateLimitResponse](s"${ghWsBaseUrl}/rate_limit", withParams = true).map {
      case serviceError: ServiceError => serviceError
      case foundRateLimit: FoundRateLimit =>
        if (foundRateLimit.rateLimitResponse.rateLimit.get.remaining > 0)
          foundRateLimit
        else ErrorMessage(RATE_LIMIT_REMAINING_ERROR)
    }
  }

  def getAllRepositories(organization: String): Future[ServiceResponse] = {
    getRepositoriesByPages(
      s"${ghWsBaseUrl}/users/${organization}/repos"
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
    * @param url the current url of the organization repositories
    * @param page the current page
    * @param foundRepositoriesAcumulator acumulator of repositories to merge with the result
    * @return `Future[ServiceResponse]` can be a `ServiceError` or `FoundRepositories`
    */
  private def getRepositoriesByPages(
      url: String,
      page: Int = 0,
      foundRepositoriesAcumulator: FoundRepositories = FoundRepositories(
        RepositoriesResponse(Some(Seq()))
      )
  ): Future[ServiceResponse] = {
    getGhApiCall[RepositoriesResponse](url, page).flatMap {
      case serviceError: ServiceError => Future.successful(serviceError)
      case notFound: NotFound         => Future.successful(notFound)
      case errorMessage: ErrorMessage => Future.successful(errorMessage)
      case foundRepositories: FoundRepositories =>
        foundRepositories.linkPages match {
          case None => // the first page is the only one
            Future.successful(
              FoundRepositories(
                RepositoriesResponse(
                  Some(
                    foundRepositories.repositoryResponse.repositories.get ++ foundRepositoriesAcumulator.repositoryResponse.repositories
                      .getOrElse(Seq())
                  )
                )
              )
            )
          case Some(linkPages) =>
            linkPages.next match {
              case None => // this is the last page because there is not next
                Future.successful(
                  FoundRepositories(
                    RepositoriesResponse(
                      Some(
                        foundRepositories.repositoryResponse.repositories.get ++ foundRepositoriesAcumulator.repositoryResponse.repositories
                          .getOrElse(Seq())
                      )
                    )
                  )
                )
              case Some(nextPage) => // move to the next page
                if (foundRepositories.retryAfter == 0) // move witout delay
                  getRepositoriesByPages(
                    url,
                    nextPage,
                    FoundRepositories(
                      RepositoriesResponse(
                        Some(
                          foundRepositories.repositoryResponse.repositories.get ++ foundRepositoriesAcumulator.repositoryResponse.repositories
                            .getOrElse(Seq())
                        )
                      )
                    )
                  )
                else { // move with delay
                  val promise = Promise[ServiceResponse]
                  system.scheduler.scheduleOnce(delay =
                    duration.Duration(foundRepositories.retryAfter, TimeUnit.SECONDS)
                  ) {
                    promise.completeWith(
                      getRepositoriesByPages(
                        url,
                        nextPage,
                        FoundRepositories(
                          RepositoriesResponse(
                            Some(
                              foundRepositories.repositoryResponse.repositories.get ++ foundRepositoriesAcumulator.repositoryResponse.repositories
                                .getOrElse(Seq())
                            )
                          )
                        )
                      )
                    )
                  }
                  promise.future
                }
            } // the first page is the only one
        }
    }
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
      foundContributorsAcumulator: FoundContributors = FoundContributors(
        ContributorsResponse(Some(Seq()))
      )
  ): Future[ServiceResponse] = {
    if (repositories.nonEmpty && index < repositories.size)
      getContributorsByPages(
        s"$ghWsBaseUrl/repos/${repositories(index).owner.login}/${repositories(index).name}/contributors",
        foundContributorsAcumulator =
          FoundContributors(ContributorsResponse(Some(Seq())), linkPages = None)
      ).flatMap {
        case serviceError: ServiceError => Future.successful(serviceError)
        case notFound: NotFound         => Future.successful(notFound)
        case errorMessage: ErrorMessage => Future.successful(errorMessage)
        case foundContributors: FoundContributors =>
          getAllContributorsForRepository(
            repositories,
            index + 1,
            FoundContributors(
              ContributorsResponse(
                Some(
                  foundContributors.contributorsResponse.contributors
                    .getOrElse(Seq()) ++ foundContributorsAcumulator.contributorsResponse.contributors
                    .getOrElse(Seq())
                )
              )
            )
          )
      }
    else Future.successful(foundContributorsAcumulator)
  }

  /**
    * This function get all the contributors from an organization recursively using the current page, calling to GitHub API and waits for the result
    *    if the result of this call is a `ServiceError` then finish,
    *    otherwise, look at the response,
    *    - if the response has a body with data, will merge it with the acumulator,
    *    - if the response have the header `link` page will continue to hit to the API with the next page number,
    *    - if the body contains data with, a smaller or equal to zero, number of elements than the page, then finish,
    *    - if the response comes with the Header `Retry-After` will wait whit the number of seconds specified.
    * @param url current repository url for his contributors
    * @param page current page
    * @param foundContributorsAcumulator acumulator of contributors to merge with the result
    * @return `Future[ServiceResponse]` can be a `ServiceError` or `FoundRepositories`
    */
  private def getContributorsByPages(
      url: String,
      page: Int = 0,
      foundContributorsAcumulator: FoundContributors
  ): Future[ServiceResponse] = {
    getGhApiCall[ContributorsResponse](
      url,
      page
    ).flatMap {
      case serviceError: ServiceError => Future.successful(serviceError)
      case notFound: NotFound         => Future.successful(notFound)
      case errorMessage: ErrorMessage => Future.successful(errorMessage)
      case foundContributors: FoundContributors =>
        foundContributors.linkPages match {
          case None => // the first page is the only one
            Future.successful(
              FoundContributors(
                ContributorsResponse(
                  Some(
                    foundContributors.contributorsResponse.contributors.get ++ foundContributorsAcumulator.contributorsResponse.contributors
                      .getOrElse(Seq())
                  )
                )
              )
            )
          case Some(linkPages) =>
            linkPages.next match {
              case None => // this is the last page because there is not next
                Future.successful(
                  FoundContributors(
                    ContributorsResponse(
                      Some(
                        foundContributors.contributorsResponse.contributors.get ++ foundContributorsAcumulator.contributorsResponse.contributors
                          .getOrElse(Seq())
                      )
                    )
                  )
                )
              case Some(nextPage) => // move to the next page
                if (foundContributors.retryAfter == 0) // move without delay
                  getContributorsByPages(
                    url,
                    nextPage,
                    FoundContributors(
                      ContributorsResponse(
                        Some(
                          foundContributors.contributorsResponse.contributors.get ++ foundContributorsAcumulator.contributorsResponse.contributors
                            .getOrElse(Seq())
                        )
                      )
                    )
                  )
                else { // move with delay
                  val promise = Promise[ServiceResponse]
                  system.scheduler.scheduleOnce(delay =
                    duration.Duration(foundContributors.retryAfter, TimeUnit.SECONDS)
                  ) {
                    promise.completeWith(
                      getContributorsByPages(
                        url,
                        nextPage,
                        FoundContributors(
                          ContributorsResponse(
                            Some(
                              foundContributors.contributorsResponse.contributors.get ++ foundContributorsAcumulator.contributorsResponse.contributors
                                .getOrElse(Seq())
                            )
                          )
                        )
                      )
                    )
                  }
                  promise.future
                }
            }
        }
    }
  }

  private def parseLinkHeader(linkHeader: String): Map[String, Int] = {
    linkHeader
      .split(',')
      .map { part =>
        val section = part.split(';')
        val url     = section(0).replace("<", "").replace(">", "")
        val page    = url.split('?')(1).split('&')(0).split('=')(1).toInt
        val name    = section(1).replace(" rel=\"", "").replace("\"", "")
        (name, page)
      }
      .toMap
  }

  private def getLinkPagesHeader(linkHeader: Option[String]): Option[LinkPages] = {
    linkHeader match {
      case Some(value) => {
        val parsedLink: Map[String, Int] = parseLinkHeader(value)
        Some(
          LinkPages(parsedLink.get(PREV),
                    parsedLink.get(FIRST),
                    parsedLink.get(NEXT),
                    parsedLink.get(LAST))
        )
      }
      case None => None
    }
  }

  /**
    * This function will parse the sequence of `Repository` or `Contributor` and return in a wrapper
    * @param response the result of the http request to GitHub API
    * @param reads implicits reads for the current data type
    * @tparam R data type of the current records expected, can be `RepositoriesResponse` or `ContributorsResponse`
    * @return a `ServiceResponse` with an error `ServiceError` if it fail, or `ErrorMessage` for status 403, or `NotFound` for status 404
    *        or a `FoundRepositories`, or `FoundContributors`, or `FoundRateLimit` if it's success
    */
  private def processJson[R](response: WSResponse)(implicit reads: Reads[R]): ServiceResponse = {
    Try(Json.parse(response.body).validate[R]) match {
      case Success(jsvalue) =>
        jsvalue match {
          case JsSuccess(responseObject, _) =>
            logger.info(s"Valid json, converte object: ${responseObject}")
            val delay = response.header(RETRY_AFTER).getOrElse("0").toInt

            responseObject match {
              case repositoryResponse: RepositoriesResponse => {
                FoundRepositories(repositoryResponse,
                                  delay,
                                  getLinkPagesHeader(response.header(LINK)))
              }
              case contributorsResponse: ContributorsResponse => {
                FoundContributors(contributorsResponse,
                                  delay,
                                  getLinkPagesHeader(response.header(LINK)))
              }
              case rateLimitResponse: RateLimitResponse => {
                FoundRateLimit(rateLimitResponse)
              }
            }

          case JsError(errors) =>
            logger.error(s"Invalid json: ${response.body}, errors: $errors")
            if (response.status == FORBIDDEN)
              ErrorMessage(RATE_LIMIT_REMAINING_ERROR_REST)
            else if (response.status == NOT_FOUND) {
              NotFound(RESOURCE_NOTFOUND)
            } else ServiceError(s"Invalid json: ${response.body}, errors: $errors")
        }
      case Failure(exception) =>
        logger.error(s"Exception when parsing json ${response.body}", exception)
        ServiceError(
          s"Exception when parsing json:${response.body}, Exception: $exception"
        )
    }
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
