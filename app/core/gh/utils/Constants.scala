package core.gh.utils

object Constants {
  object Fields {
    val NAME          = "name"
    val OWNER         = "owner"
    val LOGIN         = "login"
    val CONTRIBUTIONS = "contributions"
    val RATE          = "rate"
    val LIMIT         = "limit"
    val USED          = "used"
    val REMAINING     = "remaining"
    val RESET         = "reset"
  }

  object ControllerResults {
    val MESSAGE = "message"
    val ERRORS  = "errors"
  }

  object GhHeadersName {
    val LINK                  = "link"
    val PREV                  = "prev"
    val FIRST                 = "first"
    val NEXT                  = "next"
    val LAST                  = "last"
    val RETRY_AFTER           = "Retry-After"
    val X_RATELIMIT_RESET     = "X-RateLimit-Reset"
    val X_RATELIMIT_REMAINING = "X-RateLimit-Remaining"
  }

  object GhTokenConfig {
    val GH_ROOT                 = "gh"
    val GH_TOKEN                = s"$GH_ROOT.token.config"
    val GH_WS_TIMEOUT           = s"$GH_ROOT.ws.timeOut"
    val GH_WS_BASE_URL          = s"$GH_ROOT.ws.client.base.url"
    val GH_WS_REGISTER_PER_PAGE = s"$GH_ROOT.ws.registers.per.page"
  }

  object GhWsConstants {
    val TOKEN                    = "Token"
    val PAGE_PARAM               = "page"
    val REGISTERS_PER_PAGE_PARAM = "per_page"
  }

  object ErrorMessages {
    private val invalid                 = "error.invalid"
    val INVALID_VALUE                   = s"$invalid.value"
    val REQUIRED_FIELD                  = "error.required.field"
    val RATE_LIMIT_REMAINING_ERROR      = "rate.limit.remaining"
    val RATE_LIMIT_REMAINING_ERROR_REST = "rate.limit.remaining.rest"
    val RESOURCE_NOTFOUND               = "resource.not.found"
  }
}
