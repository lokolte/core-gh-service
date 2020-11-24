package core.gh.services

import com.google.inject.ImplementedBy
import core.gh.models.response.ServiceResponse
import core.gh.services.impl.GhServiceImpl

import scala.concurrent.Future

@ImplementedBy(classOf[GhServiceImpl])
trait GhService {
  def findRepository(organization: String): Future[ServiceResponse]
  def findContributors(organization: String): Future[ServiceResponse]
}
