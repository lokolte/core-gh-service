package core.gh.models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import core.gh.utils.Constants.Fields.{LIMIT, USED, REMAINING, RESET}
import core.gh.utils.Validators.int

case class RateLimit(limit: Int, used: Int, remaining: Int, reset: Int)

object RateLimit {
  implicit val reads: Reads[RateLimit] = (
    int(__, LIMIT) and
    int(__, USED) and
    int(__, REMAINING) and
    int(__, RESET)
  )(RateLimit.apply _)

  implicit val writes: Writes[RateLimit] = Json.writes[RateLimit]
}
