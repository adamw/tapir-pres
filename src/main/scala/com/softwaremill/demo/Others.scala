package com.softwaremill.demo

object Others {

  case class Year(year: Int)

  case class Book(title: String, year: Year)

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

  object UsingAkka extends FailFastCirceSupport {

    import scala.concurrent.Future
    import akka.http.scaladsl.server.Route
    import akka.http.scaladsl.unmarshalling.Unmarshaller
    import akka.http.scaladsl.server.Directives._
    import io.circe.generic.auto._

    def getBooks(year: Option[Year], limit: Option[Int]): Future[List[Book]] = Future.successful(Nil)

    implicit val yearUnmarshaller: Unmarshaller[String, Year] = Unmarshaller.intFromStringUnmarshaller.map(Year)

    val getBooksRoute: Route = get {
      path("books") {
        parameter("year".as[Year].?) { year =>
          parameter("limit".as[Int].?) { limit =>
            onSuccess(getBooks(year, limit)) { books =>
              complete(books)
            }
          }
        }
      }
    }
  }

  object UsingHttp4s {

    import cats.effect._
    import org.http4s._
    import org.http4s.dsl.io._
    import io.circe.generic.auto._
    import org.http4s.circe.CirceEntityEncoder._

    def getBooks(year: Option[Year], limit: Option[Int]): IO[List[Book]] = IO.pure(Nil)

    implicit val yearQueryParamDecoder: QueryParamDecoder[Year] = QueryParamDecoder[Int].map(Year)

    object OptionalYearQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Year]("year")

    object OptionalLimitQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("limit")

    val getBooksRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
      case GET -> Root / "books" :? OptionalYearQueryParamMatcher(year) +& OptionalLimitQueryParamMatcher(limit) =>
        getBooks(year, limit).flatMap(Ok(_))
    }
  }

  object UsingPlay {
    /*
      In code:
      implicit def yearPathBinder(implicit intBinder: PathBindable[Int]): PathBindable[Year] = ...

      In conf/routes:
      GET /books controllers.Api.getBooks(year: Option[Year], limit: Option[Int])
     */
  }

  object UsingJaxRs {

    import javax.ws.rs.GET
    import javax.ws.rs.Path
    import javax.ws.rs.QueryParam
    import javax.ws.rs.core.Response
    import javax.ws.rs.core.Context
    import javax.ws.rs.ext.{ParamConverterProvider, Provider}
    import javax.ws.rs.ext.Providers
    import javax.ws.rs.ext.ParamConverter
    import java.lang.annotation.Annotation
    import java.lang.reflect.Type

    @Provider
    class YearParamConverterProvider extends ParamConverterProvider {
      @Context
      val providers: Providers = ???

      override def getConverter[T](rawType: Class[T], genericType: Type, annotations: Array[Annotation]): ParamConverter[T] = ???
    }

    class BooksService {

      @GET
      @Path("/books")
      def getBooks(@QueryParam("year") year: Int, @QueryParam("limit") limit: Int): Response = {
        import javax.ws.rs.core.MediaType
        import javax.ws.rs.core.Response
        Response.status(Response.Status.OK).entity(???).`type`(MediaType.APPLICATION_JSON).build
      }
    }

  }

}
