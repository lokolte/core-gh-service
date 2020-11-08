package core.gh.models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import core.gh.utils.Constants.Fields.{NAME, OWNER}
import core.gh.utils.Validators.{isValidName, string}

case class Repository(name: String, owner: Owner)

object Repository {
  implicit val reads: Reads[Repository] = (
    string(__, NAME, validator = isValidName) and
    (__ \ OWNER).read[Owner]
  )(Repository.apply _)

  implicit val writes: Writes[Repository] = Json.writes[Repository]
}
