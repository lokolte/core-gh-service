package core.gh.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.{Lang, Langs, MessagesApi}
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents, Request, Result}
import core.gh.models
import core.gh.models.response.{ServiceError, ServiceResponse}

import scala.concurrent.{ExecutionContext, Future}
import core.gh.utils.HttpJsonResponseHelper._

@Singleton
class BaseController @Inject() (
    cc: ControllerComponents,
    langs: Langs
)(implicit ec: ExecutionContext, messagesApi: MessagesApi)
    extends AbstractController(cc) {

  implicit val lang: Lang = langs.availables.head

  def responseHandler(res: ServiceResponse): Result = res match {
    case err: ServiceError                    => InternalServerError(error(err.errMsg))
    case models.response.NotFound(errKey)     => NotFound(error(messagesApi(errKey)))
    case models.response.ErrorMessage(errKey) => Forbidden(error(messagesApi(errKey)))
  }
}
