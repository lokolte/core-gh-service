package core.gh.services

import core.gh.models.response.{ServiceError, ServiceResponse}

trait ServiceUtils {
  def recovery: PartialFunction[Throwable, ServiceResponse] = {
    case err => ServiceError(err.getMessage)
  }
}
