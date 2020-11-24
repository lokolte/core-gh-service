package core.gh.controllers

import core.gh.models.response.{FoundContributors, FoundRepositories, ServiceResponse}
import core.gh.services.GhService
import javax.inject.{Inject, Singleton}
import play.api.i18n.{Langs, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import play.api.cache.Cached

import scala.concurrent.ExecutionContext

@Singleton
class GhController @Inject() (
    cc: ControllerComponents,
    ghService: GhService,
    cached: Cached,
    langs: Langs
)(implicit ec: ExecutionContext, messagesApi: MessagesApi)
    extends BaseController(cc, langs) {

  def getGhRepositories(org: String) =
    Action
      .async {
        ghService.findRepository(org).map(ghResponseHandler)
      }

  def getGhContributors(org: String) = {
    cached
      .status(_ => org, 200) {
        Action
          .async {
            ghService.findContributors(org).map(ghResponseHandler)
          }
      }
  }

  def ghResponseHandler(response: ServiceResponse): Result = response match {
    case foundRepositories: FoundRepositories =>
      Ok(Json.toJson(foundRepositories.repositoryResponse))
    case foundContributors: FoundContributors =>
      Ok(Json.toJson(foundContributors.contributorsResponse))
    case _ => super.responseHandler(response)
  }
}
