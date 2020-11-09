package core.gh.utils;

import core.gh.utils.Constants.GhHeadersName.{LINK, RETRY_AFTER}
import core.gh.utils.GhSpecData._
import mockws.MockWS
import mockws.MockWSHelpers.Action
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.test.Helpers.GET

/**
  * This trait to mock calls to GitHub API by WSClient, here are mocked each cases as needed
  */
trait WSClientMockUtils {
  def wsClientMock(baseUrl: String) = MockWS {
    case (GET, s"${baseUrl}/rate_limit") => Action { Ok(gitHubRateLimit) }
    case (GET, s"${baseUrl}/users/owner/repos") =>
      Action { Ok(Json.toJson(gitHubRepositoriesPage1)) }
    case (GET, s"${baseUrl}/users/owner1/repos") =>
      Action { request =>
        {
          val params = request.queryString.getOrElse("page", Seq())
          if (params.nonEmpty && params.head.toInt == 0)
            Ok(Json.toJson(gitHubRepositoriesPage1)).withHeaders(
              (LINK, nextRepositoriesLinkHeaderValue)
            )
          else
            Ok(Json.toJson(gitHubRepositoriesPage2)).withHeaders(
              (LINK, lastRepositoriesLinkHeaderValue)
            )
        }
      }
    case (GET, s"${baseUrl}/users/owner2/repos") =>
      Action { request =>
        {
          val params = request.queryString.getOrElse("page", Seq())
          if (params.nonEmpty && params.head.toInt == 0)
            Ok(Json.toJson(gitHubRepositoriesPage1)).withHeaders(
              Seq((LINK, nextRepositoriesLinkHeaderValue), (RETRY_AFTER, 2.toString)): _*
            )
          else
            Ok(Json.toJson(gitHubRepositoriesPage2)).withHeaders(
              (LINK, lastRepositoriesLinkHeaderValue)
            )
        }
      }
    case (GET, s"${baseUrl}/repos/owner/repo1/contributors") =>
      Action { request =>
        {
          val params = request.queryString.getOrElse("page", Seq())
          if (params.nonEmpty && params.head.toInt == 0)
            Ok(Json.toJson(gitHubContributors)).withHeaders(
              (LINK, nextContributorLinkHeaderValue)
            )
          else
            Ok(Json.toJson(gitHubContributors)).withHeaders(
              (LINK, lastContributorLinkHeaderValue)
            )
        }
      }
    case (GET, s"${baseUrl}/repos/owner/repo2/contributors") =>
      Action { Ok(Json.toJson(gitHubContributors)) }
    case (GET, s"${baseUrl}/repos/owner/repo3/contributors") =>
      Action { Ok(Json.toJson(gitHubContributors)) }
    case (GET, s"${baseUrl}/repos/owner/repo4/contributors") =>
      Action { request =>
        {
          val params = request.queryString.getOrElse("page", Seq())
          if (params.nonEmpty && params.head.toInt == 0)
            Ok(Json.toJson(gitHubContributors)).withHeaders(
              Seq((LINK, nextContributorLinkHeaderValue), (RETRY_AFTER, 2.toString)): _*
            )
          else
            Ok(Json.toJson(gitHubContributors)).withHeaders(
              (LINK, lastContributorLinkHeaderValue)
            )
        }
      }
  }
}
