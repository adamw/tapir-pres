package com.softwaremill.demo

class SwaggerUI(yml: String) {

  import akka.http.scaladsl.model.StatusCodes
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.server.Route

  private val SwaggerYml = "swagger.yml"

  private val redirectToIndex: Route =
    redirect(s"/swagger/index.html?url=/swagger/$SwaggerYml", StatusCodes.PermanentRedirect) //

  val routes: Route =
    path("swagger") {
      redirectToIndex
    } ~
      pathPrefix("swagger") {
        path("") { // this is for trailing slash
          redirectToIndex
        } ~
          path(SwaggerYml) {
            complete(yml)
          } ~
          getFromResourceDirectory("META-INF/resources/webjars/swagger-ui/3.22.0/")
      }
}