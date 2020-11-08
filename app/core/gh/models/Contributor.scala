package core.gh.models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import core.gh.utils.Constants.Fields.{CONTRIBUTIONS, LOGIN}
import core.gh.utils.Validators.{int, isValidName, string}

case class Contributor(name: String, contributions: Int)

object Contributor {
  implicit val reads: Reads[Contributor] = (
    string(__, LOGIN, validator = isValidName) and
    int(__, CONTRIBUTIONS)
  )(Contributor.apply _)

  implicit val writes: Writes[Contributor] = Json.writes[Contributor]
}
