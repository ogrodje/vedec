import io.circe.Json
import zio.{Scope, Task, ZIO, ZLayer}
import zio.http.Header.{Accept, ContentType}
import zio.http.{Body, Client, Headers, MediaType, Request}

final class HyGraph private (
  hyGraphURL: HygraphURL,
  client: Client
):
  private def mkBody(query: String, variables: (String, Json)*): Body =
    Body.fromString(
      Json
        .obj(
          "query"     -> Json.fromString(query),
          "variables" -> Json.obj(variables*)
        )
        .noSpaces
    )

  def query(query: String, variables: (String, Json)*): Task[Json] = for
    body     <- ZIO.succeed(mkBody(query, variables*))
    request   = Request.post(hyGraphURL, body)
    response <- client.batched(request)
    rawBody  <- response.body.asString
    _        <-
      ZIO.when(!response.status.isSuccess)(
        ZIO.fail(new RuntimeException(s"Graph request has failed. Body: $rawBody"))
      )
    json     <- ZIO.fromEither(io.circe.parser.parse(rawBody))
    jsonData <-
      ZIO.fromOption((json \\ "data").headOption)
        .orElseFail(new RuntimeException("Received no data from Graph"))
  yield jsonData

object HyGraph:
  def layer: ZLayer[Scope & Client & Config, Nothing, HyGraph] = ZLayer.scoped:
    for
      hyGraphURL <- ZIO.serviceWith[Config](_.hygraphURL)
      client     <-
        ZIO.serviceWith[Client](
          _.addHeaders(
            Headers(
              Accept(MediaType.application.json),
              ContentType(MediaType.application.`json`)
            )
          )
        )
    yield new HyGraph(hyGraphURL, client)
