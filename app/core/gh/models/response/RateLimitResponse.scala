package core.gh.models.response

import play.api.libs.json.{Json, Reads, Writes, __}
import core.gh.models.{RateLimit, Repository}
import core.gh.utils.Constants.Fields.RATE

case class RateLimitResponse(rateLimit: Option[RateLimit])

object RateLimitResponse {
  implicit val reads: Reads[RateLimitResponse] =
    (__ \ RATE)
      .readNullable[RateLimit]
      .map(RateLimitResponse.apply)

  implicit val writes: Writes[RateLimitResponse] = Json.writes[RateLimitResponse]
}
