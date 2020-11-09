package core.gh.services.impl

import core.gh.models.response.{
  ContributorsResponse,
  RateLimitResponse,
  RepositoriesResponse,
  ServiceResponse
}
import core.gh.services.ServiceUtils
import core.gh.services.github.GitHubServiceImpl
import core.gh.utils.Constants.GhTokenConfig.GH_WS_BASE_URL
import javax.inject.{Inject, Singleton}
import play.api.Configuration

import scala.concurrent.Future

@Singleton
class GitHubApiCalls @Inject() (conf: Configuration, gitHubServiceImpl: GitHubServiceImpl)
    extends ServiceUtils {

  val ghWsBaseUrl: String = conf.get[String](GH_WS_BASE_URL)

  def hasRateLimit(): Future[ServiceResponse] = {
    gitHubServiceImpl
      .getGhApiCall[RateLimitResponse](s"${ghWsBaseUrl}/rate_limit", withParams = false)
  }

  def getRepositories(organization: String, page: Int): Future[ServiceResponse] = {
    gitHubServiceImpl
      .getGhApiCall[RepositoriesResponse](s"${ghWsBaseUrl}/users/${organization}/repos", page)
  }

  def getContributors(
      organization: String,
      repository: String,
      page: Int
  ): Future[ServiceResponse] = {
    gitHubServiceImpl.getGhApiCall[ContributorsResponse](
      s"$ghWsBaseUrl/repos/${organization}/${repository}/contributors",
      page
    )
  }

}
