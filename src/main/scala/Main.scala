import zio.cli.*
import zio.logging.backend.SLF4J
import zio.stream.ZPipeline
import zio.{ExitCode, Runtime, Scope, ZIOAppArgs, ZLayer}

object EpisodeToDocument:
  def pipe: ZPipeline[Any, Nothing, EpisodeJSON, DocumentWithID] =
    ZPipeline[EpisodeJSON].map: json =>
      val maybeID = json.hcursor.get[String]("id").toOption
      maybeID -> json.asObject.get.toMap.toSeq

enum Subcommand:
  case ServerCommand(config: Config) extends Subcommand
  case IndexCommand(config: Config)  extends Subcommand

object Main extends ZIOCliDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Nothing, Any] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  import ConfigAsType.given
  import Subcommand.*

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
