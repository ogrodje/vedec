import ElasticSearch.IndexName
import zio.http.Client
import zio.stream.ZStream
import zio.{Console, Runtime, Scope, ZIO, ZIOAppArgs, ZLayer}
import ZIO.logInfo

object Indexer:
  def run(config: Config) = (for
    episodes      <- ZIO.serviceWithZIO[EpisodesGraph](_.episodes())
    episodesIndex <- ZIO.service[ElasticSearchIndex]

    _ <- logInfo(s"Collected ${episodes.size} episodes")
    _ <- logInfo("Deleting index")
    _ <- episodesIndex.deleteIndex()

    _   <- logInfo("Creating index")
    out <- episodesIndex.createIndex(EpisodesIndexDefinition.definition*)
    _   <- Console.printLine(out)

    _ <-
      ZStream
        .fromIterable(episodes)
        .via(EpisodeToDocument.pipe)
        .mapZIOParUnordered(10) {
          case (sm @ Some(id), episode) =>
            episodesIndex
              .addOrUpdate(sm)(episode*)
              .zipLeft(logInfo(s"Indexed episode $id"))
          case _                        => ZIO.unit
        }
        .runDrain

    _ <- logInfo("Indexing completed.")
  yield ()).provide(
    Scope.default,
    Client.default,
    Config.fromConfig(config),
    HyGraph.layer,
    EpisodesGraph.layer,
    ElasticSearch.layer,
    ElasticSearchIndex.layer(IndexName.unsafeFrom("episodes"))
  )
