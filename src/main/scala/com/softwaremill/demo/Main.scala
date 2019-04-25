package com.softwaremill.demo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import tapir.model.StatusCode

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {

  def hello(): Unit = {
    import tapir._

    val helloEndpoint = endpoint.get.in(query[String]("name")).out(stringBody)
    println(helloEndpoint.show)

    //

    import tapir.server.akkahttp._
    val routes = helloEndpoint.toRoute(name => Future.successful(Right(s"Hello, $name!")))

    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    Await.result(Http().bindAndHandle(routes, "localhost", 8080), 1.minute)
  }

  def inputsOutputs(): Unit = {
    case class Book(title: String, year: Int)
    val booksFrom1992 = List(
      Book("Lords and Ladies", 1992),
      Book("The English Patient", 1992),
      Book("The Pelican Brief", 1992)
    )

    import tapir._
    import tapir.json.circe._
    import io.circe.generic.auto._

    val getBooksEndpoint: Endpoint[(Int, Option[Int]), String, List[Book], Nothing] = endpoint
      .get
      .in("books").in(query[Int]("fromYear")).in(query[Option[Int]]("limit"))
      .errorOut(stringBody)
      .out(jsonBody[List[Book]])

    println(getBooksEndpoint.show)
    println(getBooksEndpoint)

    //

    import tapir.server.akkahttp._
    val routes = getBooksEndpoint.toRoute { case (fromYear, limit) =>
      if (fromYear == 1992) {
        Future.successful(Right(booksFrom1992.take(limit.getOrElse(10))))
      } else {
        Future.successful(Left("unknown year"))
      }
    }

    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    Await.result(Http().bindAndHandle(routes, "localhost", 8080), 1.minute)
  }

  def manyEndpoints(): Unit = {
    type AuthToken = String
    type ErrorMsg = String

    case class Year(year: Int)
    case class Book(title: String, year: Year)
    var books = List(
      Book("Lords and Ladies", Year(1992)),
      Book("The Sorrows of Young Werther", Year(1774)),
      Book("Iliad", Year(-8000)),
      Book("Nad Niemnem", Year(1888)),
      Book("The Art of Computer Programming", Year(1968)),
      Book("Pharaoh", Year(1897))
    )

    import io.circe.generic.auto._
    import tapir._
    import tapir.json.circe._

    implicit val yearCodec: Codec[Year, MediaType.TextPlain, String] = Codec.intPlainCodec.map(Year)(_.year)

    //

    val getBooksEndpoint: Endpoint[(Year, Option[Int]), ErrorMsg, List[Book], Nothing] = endpoint
      .get
      .in("books").in(query[Year]("fromYear")).in(query[Option[Int]]("limit"))
      .errorOut(stringBody)
      .out(jsonBody[List[Book]])

    val addBookEndpoint: Endpoint[(Book, AuthToken), ErrorMsg, Unit, Nothing] = endpoint
      .post
      .in("books" / "add").in(jsonBody[Book])
      .errorOut(stringBody)
      .in(auth.bearer)

    //

    case class BookFilter(year: Option[Year], limit: Option[Int])
    val yearInput = query[Option[Year]]("fromYear")
    val bookFilterInput: EndpointInput[BookFilter] = yearInput.and(query[Option[Int]]("limit")).mapTo(BookFilter)

    //

    val baseEndpoint: Endpoint[Unit, (StatusCode, ErrorMsg), Unit, Nothing] =
      endpoint.in("books").errorOut(statusCode.and(stringBody))

    val getBooksEndpoint2: Endpoint[BookFilter, (StatusCode, ErrorMsg), List[Book], Nothing] = baseEndpoint
      .get
      .in(bookFilterInput)
      .out(jsonBody[List[Book]])

    val addBookEndpoint2: Endpoint[(Book, AuthToken), (StatusCode, ErrorMsg), Unit, Nothing] = baseEndpoint
      .post
      .in("add").in(jsonBody[Book])
      .in(auth.bearer)

    //

    import tapir.server.akkahttp._
    val getBookRoute = getBooksEndpoint2.toRoute { filter =>
      val books1 = filter.year.map(year => books.filter(_.year == year)).getOrElse(books)
      val books2 = filter.limit.map(limit => books1.take(limit)).getOrElse(books1)
      Future.successful(Right(books2))
    }
    val addBookRoute = addBookEndpoint2.toRoute { case (book, authToken) =>
      if (authToken == "Secret") {
        Future {
          books = book :: books
          Right(())
        }
      } else {
        Future.successful(Left((403, "Wrong token!")))
      }
    }

    import akka.http.scaladsl.server.Directives._
    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    Await.result(Http().bindAndHandle(getBookRoute ~ addBookRoute, "localhost", 8080), 1.minute)
  }

  def docs(): Unit = {
    type AuthToken = String
    type ErrorMsg = String

    case class Year(year: Int)
    case class Book(title: String, year: Year)
    var books = List(
      Book("Lords and Ladies", Year(1992)),
      Book("The Sorrows of Young Werther", Year(1774)),
      Book("Iliad", Year(-8000)),
      Book("Nad Niemnem", Year(1888)),
      Book("The Art of Computer Programming", Year(1968)),
      Book("Pharaoh", Year(1897))
    )

    import io.circe.generic.auto._
    import tapir._
    import tapir.json.circe._

    implicit val yearCodec: Codec[Year, MediaType.TextPlain, String] = Codec.intPlainCodec.map(Year)(_.year)

    //

    case class BookFilter(year: Option[Year], limit: Option[Int])
    val yearInput = query[Option[Year]]("fromYear")
    val bookFilterInput: EndpointInput[BookFilter] = yearInput.and(query[Option[Int]]("limit")).mapTo(BookFilter)

    //

    val baseEndpoint: Endpoint[Unit, (StatusCode, ErrorMsg), Unit, Nothing] =
      endpoint.in("books").errorOut(statusCode.and(stringBody))

    val getBooksEndpoint: Endpoint[BookFilter, (StatusCode, ErrorMsg), List[Book], Nothing] = baseEndpoint
      .get
      .in(bookFilterInput)
      .out(jsonBody[List[Book]])

    val addBookEndpoint: Endpoint[(Book, AuthToken), (StatusCode, ErrorMsg), Unit, Nothing] = baseEndpoint
      .post
      .in("add")
      .in(jsonBody[Book].description("The book to add").example(Book("Pride and Prejudice", Year(1813))))
      .in(auth.bearer)

    //

    import tapir.server.akkahttp._
    val getBookRoute = getBooksEndpoint.toRoute { filter =>
      val books1 = filter.year.map(year => books.filter(_.year == year)).getOrElse(books)
      val books2 = filter.limit.map(limit => books1.take(limit)).getOrElse(books1)
      Future.successful(Right(books2))
    }
    val addBookRoute = addBookEndpoint.toRoute { case (book, authToken) =>
      if (authToken == "Secret") {
        Future {
          books = book :: books
          Right(())
        }
      } else {
        Future.successful(Left((403, "Wrong token!")))
      }
    }

    //

    import tapir.docs.openapi._
    import tapir.openapi.circe.yaml._

    val docs = List(getBooksEndpoint, addBookEndpoint).toOpenAPI("The Tapir Library", "1.0")
    val yml = docs.toYaml

    //

    import akka.http.scaladsl.server.Directives._
    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    Await.result(Http().bindAndHandle(
      getBookRoute ~ addBookRoute ~ new SwaggerUI(yml).routes,
      "localhost", 8080), 1.minute)
  }

  //hello()
  //inputsOutputs()
  //manyEndpoints()
  docs()
}
