package core.gh.services.github

import akka.actor.ActorSystem
import core.gh.models.response.{
  ContributorsResponse,
  ErrorMessage,
  FoundContributors,
  FoundRateLimit,
  FoundRepositories,
  NoContent,
  NotFound,
  RateLimitResponse,
  RepositoriesResponse,
  ServiceError,
  ServiceResponse
}
import core.gh.services.ServiceUtils
import core.gh.utils.Constants.ErrorMessages.{RATE_LIMIT_REMAINING_ERROR_REST, RESOURCE_NOTFOUND}
import core.gh.utils.Constants.GhHeadersName.{LINK, RETRY_AFTER}
import core.gh.utils.Constants.GhTokenConfig.{GH_TOKEN, GH_WS_REGISTER_PER_PAGE, GH_WS_TIMEOUT}
import core.gh.utils.Constants.GhWsConstants.{PAGE_PARAM, REGISTERS_PER_PAGE_PARAM, TOKEN}
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logging}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.libs.json._
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION}
import play.api.http.ContentTypes.JSON
import play.api.http.Status.{FORBIDDEN, NOT_FOUND, NO_CONTENT}
import play.api.mvc.ControllerComponents
import play.api.libs.ws.WSResponse

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

@Singleton
class GitHubServiceImpl @Inject() (conf: Configuration,
                                   ws: WSClient,
                                   val controllerComponents: ControllerComponents)(
    implicit val system: ActorSystem,
    ec: ExecutionContext
) extends ServiceUtils
    with Logging {

  val timeOut: Int        = conf.get[Int](GH_WS_TIMEOUT)
  val ghToken: String     = conf.get[String](GH_TOKEN)
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

  def getGhApiCall[R](url: String, page: Int = 0, withParams: Boolean = true)(
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
    ).map(processJson[R])
      .recover(recovery)
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
    if (response.status == FORBIDDEN)
      ErrorMessage(RATE_LIMIT_REMAINING_ERROR_REST)
    else if (response.status == NOT_FOUND)
      NotFound(RESOURCE_NOTFOUND)
    else if (response.status == NO_CONTENT)
      NoContent()
    else
      Try(Json.parse(response.body).validate[R]) match {
        case Success(jsvalue) =>
          jsvalue match {
            case JsSuccess(responseObject, _) =>
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
              ServiceError(s"Invalid json: ${response.body}, errors: $errors")
          }
        case Failure(exception) =>
          logger.error(s"Exception when parsing json ${response.body}", exception)
          ServiceError(
            s"Exception when parsing json:${response.body}, Exception: $exception"
          )
      }
  }
}
