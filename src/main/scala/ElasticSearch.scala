import eu.timepit.refined.auto.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV
import io.circe.Json
import zio.{Chunk, Task, ZIO, ZLayer}
import zio.http.Header.{Accept, ContentType}
import zio.http.{Body, Client, Header, Headers, MediaType, Path, Request}

type IndexName = String Refined NonEmpty

final class ElasticSearch private (
  private val client: Client,
  private val esURL: ElasticSerchURL,
  private val elasticSearchPassword: Password
):
  private def requestAndHandle(request: Request): ZIO[Any, Throwable, Json] = for
    response <- client.batched(request)
    body     <- response.body.asString
    json     <- ZIO.fromEither(io.circe.parser.parse(body))
  yield json

  private def payloadToBody(payload: (String, Json)*): Task[Body] =
    ZIO.succeed(Body.fromString(Json.fromFields(payload.toSeq).noSpaces))

  def search(indexNames: IndexName*)(payload: (String, Json)*): Task[Json] = for
    body   <- payloadToBody(payload*)
    request = Request.post(esURL, body).updatePath(_./("_search"))
    json   <- requestAndHandle(request)
  yield json

  def addOrUpdate(indexName: IndexName, maybeID: Option[String] = None)(payload: (String, Json)*): Task[Json] = for
    body          <- payloadToBody(payload*)
    maybeContentID = payload.find(_._1 == "id").flatMap(_._2.asString)
    path           =
      maybeID
        .orElse(maybeContentID)
        .fold((p: Path) => p / indexName / "_doc")(id => (p: Path) => p / indexName / "_doc" / id)

    request =
      maybeID
        .orElse(maybeContentID)
        .fold(
          Request.post(esURL, body).updatePath(path).setQueryParams("op_type" -> Chunk[String]("create"))
        )(id => Request.put(esURL, body).updatePath(path))

    json <- requestAndHandle(request)
  yield json

  def deleteIndex(indexName: IndexName): ZIO[Any, Throwable, Json] =
    requestAndHandle(Request.delete(esURL).updatePath(_ / indexName))

  def createIndex(indexName: IndexName)(payload: (String, Json)*): Task[Json] = for
    body <- payloadToBody(payload*)
    json <- requestAndHandle(Request.put(esURL, body).updatePath(_ / indexName))
  yield json

object ElasticSearch:

  object IndexName:
    def unsafeFrom(name: String): IndexName = refineV[NonEmpty](name).toOption.get

  val all: IndexName = IndexName.unsafeFrom("_all")

  def layer = ZLayer.scoped:
    for
      (searchURL, (username, password)) <-
        ZIO.serviceWith[Config](c => c.elasticSearchURL -> (c.elasticSearchUsername, c.elasticSearchPassword))
      client                            <-
        ZIO.serviceWith[Client](
          _.addHeaders(
            Headers(
              Accept(MediaType.application.json),
              ContentType(MediaType.application.`json`),
              Header.Authorization.Basic(username, password)
            )
          )
        )
    yield new ElasticSearch(client, searchURL, password)

final case class ElasticSearchIndex private (
  private val es: ElasticSearch,
  private val indexName: IndexName
):
  def search(payload: (String, Json)*): Task[Json]                          = es.search(indexName)(payload*)
  def addOrUpdate(id: Option[String])(payload: (String, Json)*): Task[Json] = es.addOrUpdate(indexName, id)(payload*)
  def deleteIndex(): Task[Json]                                             = es.deleteIndex(indexName)
  def createIndex(payload: (String, Json)*): Task[Json]                     = es.createIndex(indexName)(payload*)

object ElasticSearchIndex:
  def layer(indexName: IndexName) = ZLayer.scoped:
    for es <- ZIO.service[ElasticSearch]
    yield new ElasticSearchIndex(es, indexName)
