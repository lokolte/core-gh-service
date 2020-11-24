package core.gh.models.response

import core.gh.models.response.headers.LinkPages

trait ServiceResponse

final case class ServiceError(errMsg: String) extends ServiceResponse

final case class NotFound(errorKey: String) extends ServiceResponse

final case class ErrorMessage(errorKey: String) extends ServiceResponse

final case class FoundContributors(contributorsResponse: ContributorsResponse,
                                   retryAfter: Int = 0,
                                   linkPages: Option[LinkPages] = None)
    extends ServiceResponse

final case class FoundRepositories(repositoryResponse: RepositoriesResponse,
                                   retryAfter: Int = 0,
                                   linkPages: Option[LinkPages] = None)
    extends ServiceResponse

final case class FoundRateLimit(rateLimitResponse: RateLimitResponse) extends ServiceResponse
