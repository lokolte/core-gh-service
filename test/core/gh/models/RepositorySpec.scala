package core.gh.models

import core.gh.models.RepositorySpec.repoTestJson
import core.gh.utils.Constants.ErrorMessages.REQUIRED_FIELD
import core.gh.utils.Constants.Fields.NAME
import core.gh.utils.ValidationUtils
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsObject, JsResult, JsString, Json}

class RepositorySpec extends WordSpec with MustMatchers with ValidationUtils {

  "PIISpec" should {

    "succeed for a valid values" in {
      val result: JsResult[Repository] = repoTestJson.validate[Repository]
      result.isSuccess must be(true)
    }

    "fail for invalid empty name" in {
      val result: JsResult[Repository] =
        (repoTestJson + (NAME -> JsString(""))).validate[Repository]
      result.isError must be(true)
      validateJsError(result, NAME, REQUIRED_FIELD)
    }
  }
}

object RepositorySpec {
  val repoTestData = Repository(
    name = "repo",
    owner = Owner(login = "owner")
  )
  val repoTestJson: JsObject = Json.toJson(repoTestData).as[JsObject]
}
