package com.softwaremill.demo

object Client extends App {
  import com.softwaremill.sttp._
  import io.circe.generic.auto._
  import tapir._
  import tapir.json.circe._
  import tapir.client.sttp._

  //

  type ErrorMsg = String

  case class Year(year: Int)
  case class Book(title: String, year: Year)

  implicit val yearCodec: Codec[Year, MediaType.TextPlain, String] = Codec.intPlainCodec.map(Year)(_.year)

  case class BookFilter(year: Option[Year], limit: Option[Int])
  val yearInput = query[Option[Year]]("fromYear")
  val bookFilterInput: EndpointInput[BookFilter] = yearInput.and(query[Option[Int]]("limit")).mapTo(BookFilter)

  val baseEndpoint: Endpoint[Unit, (StatusCode, ErrorMsg), Unit, Nothing] =
    endpoint.in("books").errorOut(statusCode.and(stringBody))

  val getBooksEndpoint: Endpoint[BookFilter, (StatusCode, ErrorMsg), List[Book], Nothing] = baseEndpoint
    .get
    .in(bookFilterInput)
    .out(jsonBody[List[Book]])

  //

  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  val getBooksRequest: Request[Either[(StatusCode, String), List[Book]], Nothing] = getBooksEndpoint
    .toSttpRequest(uri"http://localhost:8080")
    .apply(BookFilter(None, None))

  println("Result: " + getBooksRequest.send().unsafeBody)
}