package core.gh.services

import core.gh.models.response.headers.LinkPages
import core.gh.models.response.{ServiceError, ServiceResponse}
import core.gh.utils.Constants.GhHeadersName.{FIRST, LAST, NEXT, PREV}

trait ServiceUtils {
  def recovery: PartialFunction[Throwable, ServiceResponse] = {
    case err => ServiceError(err.getMessage)
  }

  def parseLinkHeader(linkHeader: String): Map[String, Int] = {
    linkHeader
      .split(',')
      .map { part =>
        val section = part.split(';')
        val url     = section(0).replace("<", "").replace(">", "")
        val page    = url.split('?')(1).split('&')(0).split('=')(1).toInt
        val name    = section(1).replace(" rel=\"", "").replace("\"", "")
        (name, page)
      }
      .toMap
  }

  def getLinkPagesHeader(linkHeader: Option[String]): Option[LinkPages] = {
    linkHeader match {
      case Some(value) => {
        val parsedLink: Map[String, Int] = parseLinkHeader(value)
        Some(
          LinkPages(parsedLink.get(PREV),
                    parsedLink.get(FIRST),
                    parsedLink.get(NEXT),
                    parsedLink.get(LAST))
        )
      }
      case None => None
    }
  }
}
