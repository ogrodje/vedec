import ElasticSearch.IndexName
import zio.{Console, ExitCode, Runtime, Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}
import zio.stream.{ZPipeline, ZStream}
import zio.http.*
import io.circe.Json
import zio.cli.ZIOCliDefault
import zio.cli.*
import zio.cli.HelpDoc.Span.text
import zio.Console.printLine
import zio.cli.ValidationErrorType.InvalidValue
import zio.logging.backend.SLF4J
import scala.deriving.Mirror
object EpisodeToDocument:
  def pipe: ZPipeline[Any, Nothing, EpisodeJSON, DocumentWithID] =
    ZPipeline[EpisodeJSON].map: json =>
      val maybeID = json.hcursor.get[String]("id").toOption
      maybeID -> json.asObject.get.toMap.toSeq

object Main2 extends ZIOAppDefault:
  override def run = (for
    episodesGraph <- ZIO.service[EpisodesGraph]
    episodes      <- episodesGraph.episodes()
    episodesIndex <- ZIO.service[ElasticSearchIndex]

    _   <- episodesIndex.deleteIndex()
    out <- episodesIndex.createIndex(EpisodesIndexDefinition.definition*)
    _   <- Console.printLine(out)

    _ <-
      ZStream
        .fromIterable(episodes)
        .via(EpisodeToDocument.pipe)
        .mapZIOParUnordered(10)((id, episode) => episodesIndex.addOrUpdate(id)(episode*))
        .runDrain

    _ <- Console.printLine("Indexing completed.")
  yield ()).provide(
    Scope.default,
    Client.default,
    Config.layer,
    HyGraph.layer,
    ElasticSearch.layer,
    EpisodesGraph.layer,
    ElasticSearchIndex.layer(IndexName.unsafeFrom("episodes"))
  )

enum Subcommand:
  case ServerCommand(config: Config) extends Subcommand
  case IndexCommand(config: Config)  extends Subcommand

object Main extends ZIOCliDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Nothing, Any] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  import Subcommand.*
  import ConfigAsType.given

  private val serverCommand: Command[ConfigAsType] = Command(
    "server",
    ConfigOptions.options,
    Args.none
  ).withHelp(HelpDoc.p("Runs the server"))

  private val indexCommand: Command[ConfigAsType] = Command(
    "index",
    ConfigOptions.options,
    Args.none
  ).withHelp(HelpDoc.p("Index the documents and services"))

  private val runCommand: Command[Unit] = Command("run").withHelp(HelpDoc.p("Run the sub-command"))

  private val command = runCommand.subcommands(
    serverCommand.map(ServerCommand.apply(_)),
    indexCommand.map(IndexCommand(_))
  )

  val cliApp: CliApp[ZIOAppArgs & Scope, Nothing, ExitCode] = CliApp.make(
    name = "vedec",
    version = "0.0.1",
    summary = HelpDoc.Span.text("Main entrypoint for vedec."),
    command = command
  ) {
    case cmd: ServerCommand => runServer(cmd)
    case cmd: IndexCommand  => runIndexer(cmd)
  }

  private def runServer(cmd: ServerCommand) = VedecServer.run(cmd.config).exitCode
  private def runIndexer(cmd: IndexCommand) = Indexer.run(cmd.config).exitCode
