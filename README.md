# GitHub API contributions from org
An organization is an owner of a group of repositories in GitHub, in order to get all the contributors with the number of the contributions, you can use this api, and will return all the contributors from an organization including their name and contributions ordered by the number of contributions in repositories of this organization.

### To Run Locally
Follow these steps:
 1. `docker-compose up`
 2. `sbt run`
 3. Debug mode `sbt run -jvm-debug $port` and configure the same port for your ide

### Endpoints

| Method | URL												| Controller & Params				                    |	Description
|------|----------------------------------------------------|-------------------------------------------------------|----------------------------------------------------------------
| GET 	|	`/org/:org/repositories `   	    			| `GhController.getGhRepositories(org: String)`         | Get all repositories given its organization name
| GET 	|	`/org/:org/contributors `   	    			| `GhController.getGhContributors(org: String)`         | Get all contributors given its organization name, these results are cached in memory

### Endpoints Documentations
This project use OAS3 (OpenAPI Specification 3) from Swagger.

After started the aplication you can se the docs in:
1. Running with `docker-compose up` docs in [Docs](http://localhost:8080/gh/docs)
2. Running with `sbt run` or `sbt run -jvm-debug $port` docs in [Docs](http://localhost:9000/gh/docs)

### Versioning
This repo uses OneFlow branching model, which the version is considered just for branches with name patron `release/*`, the endpoint for version is `GET /gh/version` and return the version of the current release.

### Run PreProduction mode
In order to run in production mode we must follow those task:
1. Build the universal package zip by the command `sbt dist`
2. Update the `GH_TOKEN` on `docker/gh.env` file
2. Build the image and start by `docker-compose up`

### Unit tests
To run unit tests and generate coverage report, run:
1. `sbt clean coverage test coverageReport`

### Quick
Import postman.json file in your postman to test it directly.