package core.gh.models.response

import play.api.libs.json.Json

case class Error(message: String)

object Error {
  implicit val errorWrites = Json.writes[Error]
}

case class Errors(errors: Seq[Error])

object Errors {
  implicit val errorsWrites = Json.writes[Errors]
}
