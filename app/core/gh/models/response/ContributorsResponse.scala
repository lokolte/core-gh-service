package core.gh.models.response

import play.api.libs.json.{Json, Reads, Writes, __}
import core.gh.models.Contributor

case class ContributorsResponse(contributors: Option[Seq[Contributor]])

object ContributorsResponse {
  implicit val reads: Reads[ContributorsResponse] =
    __.readNullable(Reads.seq[Contributor])
      .map(ContributorsResponse.apply)

  implicit val writes: Writes[ContributorsResponse] =
    (contributorsResponse: ContributorsResponse) =>
      Json.toJson(contributorsResponse.contributors.getOrElse(Seq()))
}
