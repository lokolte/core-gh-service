package core.gh.controllers

import play.api.libs.json.JsValue
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, JSON}

object TestUtils {
  def fakeRequestWithBody(httpVerb: String, path: String, json: JsValue): FakeRequest[JsValue] =
    fakeRequestWithoutBody(httpVerb, path).withBody(json)

  def fakeRequestWithoutBody(httpVerb: String, path: String) =
    FakeRequest(httpVerb, path)
      .withHeaders((CONTENT_TYPE, JSON))
}
