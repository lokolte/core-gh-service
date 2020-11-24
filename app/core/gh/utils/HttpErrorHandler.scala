package core.gh.utils

import javax.inject.{Singleton, _}
import play.api.http.{JsonHttpErrorHandler, Status}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Environment, Logging, OptionalSourceMapper}
import HttpJsonResponseHelper._

import scala.concurrent._

@Singleton
class HttpErrorHandler @Inject() (
    env: Environment,
    sourceMapper: OptionalSourceMapper
) extends JsonHttpErrorHandler(env, sourceMapper)
    with Status
    with Logging {

  override def onClientError(request: RequestHeader,
                             statusCode: Int,
                             message: String): Future[Result] = {

    statusCode match {
      case 400 => Future.successful(BadRequest(error(message)))
      case 404 => Future.successful(NotFound(error(request.path)))
      case _ =>
        Future.successful(UnprocessableEntity(error(message)))
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(
      InternalServerError(error(exception.getMessage))
    )
  }
}
