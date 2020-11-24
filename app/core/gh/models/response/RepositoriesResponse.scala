package core.gh.models.response

import play.api.libs.json.{Json, Reads, Writes, __}
import core.gh.models.Repository

case class RepositoriesResponse(repositories: Option[Seq[Repository]])

object RepositoriesResponse {
  implicit val reads: Reads[RepositoriesResponse] =
    __.readNullable(Reads.seq[Repository])
      .map(RepositoriesResponse.apply)

  implicit val writes: Writes[RepositoriesResponse] =
    (repositoryResponse: RepositoriesResponse) =>
      Json.toJson(repositoryResponse.repositories.getOrElse(Seq()))
}
