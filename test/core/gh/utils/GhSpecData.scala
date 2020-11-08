package core.gh.utils

import core.gh.models.{Contributor, Owner, RateLimit, Repository}
import core.gh.models.response.{
  ContributorsResponse,
  ErrorMessage,
  FoundContributors,
  FoundRateLimit,
  FoundRepositories,
  NotFound,
  RateLimitResponse,
  RepositoriesResponse,
  ServiceError
}
import core.gh.utils.Constants.ErrorMessages.{
  RATE_LIMIT_REMAINING_ERROR,
  RATE_LIMIT_REMAINING_ERROR_REST,
  RESOURCE_NOTFOUND
}
import play.api.libs.json.Json

object GhSpecData {
  val contributor1 = Contributor("cont1", 1)
  val contributor2 = Contributor("cont2", 2)
  val contributor3 = Contributor("cont3", 3)

  val uniqueContributorsByName =
    Seq(Contributor("cont1", 2), Contributor("cont2", 4), Contributor("cont3", 6))

  val contributorsResponse = ContributorsResponse(
    Some(Seq(contributor1, contributor2, contributor3))
  )

  val foundContributors = FoundContributors(contributorsResponse)

  val gitHubContributors = Json.parse(
    """[{"login":"cont1","contributions":1},{"login":"cont2","contributions":2},{"login":"cont3","contributions":3}]"""
  )

  val nextContributorLinkHeaderValue =
    "<https://api.github.com/repositories/repo1/contributors?page=2&per_page=1>; rel=\"next\", <https://api.github.com/repositories/repo1/contributors?page=2&per_page=1>; rel=\"last\""
  val lastContributorLinkHeaderValue =
    "<https://api.github.com/repositories/repo1/contributors?page=1&per_page=1>; rel=\"prev\", <https://api.github.com/repositories/repo1/contributors?page=1&per_page=1>; rel=\"first\""

  val contributorsResponseProcessed = ContributorsResponse(
    Some(
      Seq(
        contributor1,
        contributor2,
        contributor3,
        contributor1,
        contributor2,
        contributor3,
        contributor1,
        contributor2,
        contributor3,
        contributor1,
        contributor2,
        contributor3
      )
    )
  )

  val foundContributorsProcessed = FoundContributors(contributorsResponseProcessed)

  val foundContributorsWithDelay = FoundContributors(
    ContributorsResponse(
      Some(Seq(contributor1, contributor2, contributor3, contributor1, contributor2, contributor3))
    )
  )

  val repeatedContributors = foundContributorsWithDelay.contributorsResponse.contributors.get

  val owner       = Owner("owner")
  val repository1 = Repository("repo1", owner)
  val repository2 = Repository("repo2", owner)
  val repository3 = Repository("repo3", owner)
  val repository4 = Repository("repo4", owner)

  val repositoriesResponse = RepositoriesResponse(Some(Seq(repository1, repository2, repository3)))
  val foundRepositories    = FoundRepositories(repositoriesResponse)

  val gitHubRepositoriesPage1 =
    Json.parse(
      """[{"name":"repo2","owner":{"login":"owner"}},{"name":"repo3","owner":{"login":"owner"}}]"""
    )
  val gitHubRepositoriesPage2 = Json.parse("""[{"name":"repo1","owner":{"login":"owner"}}]""")

  val nextRepositoriesLinkHeaderValue =
    "<https://api.github.com/user/organization/repos?page=2&per_page=2>; rel=\"next\", <https://api.github.com/user/organization/repos?page=2&per_page=2>; rel=\"last\""
  val lastRepositoriesLinkHeaderValue =
    "<https://api.github.com/user/organization/repos?page=1&per_page=2>; rel=\"prev\", <https://api.github.com/user/organization/repos?page=1&per_page=2>; rel=\"first\""

  val rateLimit         = RateLimit(5000, 495, 5, 1604831447)
  val rateLimitResponse = RateLimitResponse(Some(rateLimit))

  val foundRateLimit = FoundRateLimit(rateLimitResponse)

  val gitHubRateLimit =
    Json.parse(
      """{"resources":{},"rate":{"limit":5000,"remaining":5,"reset": 1604831447,"used":495}}""".stripMargin
    )

  val serviceError = ServiceError("Invalid json: {someBadJson}")

  val notFound = NotFound(RESOURCE_NOTFOUND)

  val errorMessage1 = ErrorMessage(RATE_LIMIT_REMAINING_ERROR)
  val errorMessage2 = ErrorMessage(RATE_LIMIT_REMAINING_ERROR_REST)

}
