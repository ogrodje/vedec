import ElasticSearch.IndexName
import zio.{Scope, Task, URIO, ZIO, ZLayer}
import zio.http.*
import eu.timepit.refined.auto.*
import io.circe.Json
import zio.ZIO.{logError, logInfo}
import zio.http.Header.AccessControlAllowOrigin
import zio.http.Middleware.{cors, CorsConfig}

final case class VedecServer(private val config: Config):
  private val corsConfig: CorsConfig =
    CorsConfig(allowedOrigin = _ => Some(AccessControlAllowOrigin.All))

  private def bodyToJson(body: Body): Task[Json] =
    body.asString.flatMap(json => ZIO.fromEither(io.circe.parser.parse(json)))

  private def jsonToDoc(json: Json): Seq[(String, Json)] =
    json.asObject.map(_.toMap.toSeq).getOrElse(Seq.empty)

  private val routes: Routes[ElasticSearchIndex, Nothing] = Routes(
    Method.GET / "search" -> handler { (req: Request) =>
      for
        body       <- bodyToJson(req.body).map(jsonToDoc)
        esResponse <- ZIO.serviceWithZIO[ElasticSearchIndex](_.search(body*))
        response    = Response.json(esResponse.noSpaces)
        _          <- logInfo("Served search request")
      yield response
    },
    Method.GET / "query"  -> handler { (req: Request) =>
      val rawQuery = req.queryParamOrElse("query", "")
      val query    = EpisodesIndexQueries.query(rawQuery)
      for
        esResponse <- ZIO.serviceWithZIO[ElasticSearchIndex](_.search(query*))
        response    = Response.json(esResponse.noSpaces)
        _          <- logInfo(s"Served query request for $rawQuery")
      yield response
    }
  ).handleErrorZIO(e => logError(s"Boom w/ $e").as(Response.badRequest(s"Boom w/ $e"))) @@ cors(corsConfig)

  def server: URIO[ElasticSearchIndex & Server, Nothing] = Server.serve(routes)

object VedecServer:
  def run(config: Config) =
    ZIO.logInfo(s"Booting server on port ${config.port}") *>
      VedecServer(config).server
        .provide(
          Config.fromConfig(config),
          Client.default,
          Server.defaultWithPort(config.port),
          ElasticSearch.layer,
          ElasticSearchIndex.layer(IndexName.unsafeFrom("episodes"))
        )
