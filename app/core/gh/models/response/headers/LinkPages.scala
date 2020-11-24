package core.gh.models.response.headers

case class LinkPages(prev: Option[Int] = None,
                     first: Option[Int] = None,
                     next: Option[Int] = None,
                     last: Option[Int] = None)
