package core.gh.models

import play.api.libs.json._
import core.gh.utils.Constants.Fields.LOGIN
import core.gh.utils.Validators.{isValidName, string}

case class Owner(login: String)

object Owner {
  implicit val reads: Reads[Owner] =
    string(__, LOGIN, validator = isValidName).map(Owner.apply)

  implicit val writes: Writes[Owner] = Json.writes[Owner]
}
