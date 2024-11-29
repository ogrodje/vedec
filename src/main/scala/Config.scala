import eu.timepit.refined.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.*
import eu.timepit.refined.numeric.*
import eu.timepit.refined.collection.*
import zio.cli.ValidationErrorType.InvalidValue
import zio.cli.{HelpDoc, Options, ValidationError}
import zio.{IO, TaskLayer, ULayer, ZLayer}
import zio.prelude.Validation
import zio.http.*

import scala.deriving.Mirror
import scala.util.Try
import scala.util.control.NoStackTrace

type Port            = Int Refined Positive
type Password        = String Refined NonEmpty
type Username        = String Refined NonEmpty
type HygraphURL      = URL
type ElasticSerchURL = URL

type ConfigAsType = (Port, ElasticSerchURL, Username, Password, HygraphURL)
object ConfigAsType:
  private val toConfig: ConfigAsType => Config =
    tuple => summon[Mirror.Of[Config]].fromProduct(tuple)

  given Conversion[ConfigAsType, Config] = tuple => toConfig(tuple)

final case class Config(
  port: Port,
  elasticSearchURL: ElasticSerchURL,
  elasticSearchUsername: Username,
  elasticSearchPassword: Password,
  hygraphURL: HygraphURL
)

object ConfigOptions:
  private def asURL(raw: String): Either[ValidationError, URL] =
    URL.decode(raw).left.map(ec => ValidationError(InvalidValue, HelpDoc.p("Invalid URL")))

  private def asPort(n: BigInt): Either[ValidationError, Port] =
    Try(n.toInt).toEither.fold(
      th => Left(ValidationError(InvalidValue, HelpDoc.p("Invalid PORT"))),
      port => refineV[Positive](port).left.map(ec => ValidationError(InvalidValue, HelpDoc.p("Invalid PORT")))
    )

  private def asNotEmpty[O](raw: String): Either[ValidationError, String Refined NonEmpty] =
    refineV[NonEmpty](raw).left.map(ec => ValidationError(InvalidValue, HelpDoc.p("Missing value")))

  def options =
    Options.integer("port").alias("P").withDefault(BigInt(7777)).mapOrFail(asPort) ++
      Options.text("elasticSearchURL").withDefault("http://localhost:9200").mapOrFail(asURL) ++
      Options.text("elasticSearchUsername").withDefault("elastic").mapOrFail(asNotEmpty[Username]) ++
      Options.text("elasticSearchPassword").mapOrFail(asNotEmpty[Password]) ++
      Options.text("hygraphURL").mapOrFail(asURL)

enum ConfigError(message: String) extends Throwable(message) with NoStackTrace:
  case Missing(name: String)            extends ConfigError(s"Missing or problem reading environment variable $name")
  case InvalidPort(message: String)     extends ConfigError("Invalid port number")
  case InvalidURL(message: String)      extends ConfigError("Invalid URL")
  case InvalidPassword(message: String) extends ConfigError("Invalid password format")
  case InvalidUsername(message: String) extends ConfigError("Invalid username")

object Config:
  import ConfigError.*

  private def readRequired(name: String): Validation[Missing, String] =
    Validation.fromOption(Option(System.getenv(name))).mapError(_ => Missing(name))

  private def readURL(raw: String): Validation[InvalidURL, URL] =
    Validation.fromEither(URL.decode(raw)).mapError(e => InvalidURL(e.getMessage))

  private def readPort(raw: String): Validation[InvalidPort, Port] =
    Try(raw.toInt).toEither.fold(
      error => Validation.fail(InvalidPort(error.getMessage)),
      port => Validation.fromEither(refineV[Positive](port)).mapError(InvalidPort.apply)
    )

  private def readPassword(raw: String): Validation[InvalidPassword, Password] =
    Validation.fromEither(refineV[NonEmpty](raw)).mapError(err => InvalidPassword(err))

  private def validationFromEnvironment: Validation[ConfigError, Config] = Validation.validateWith(
    readRequired("PORT").flatMap(readPort),
    readRequired("ELASTICSEARCH_URL").flatMap(readURL),
    readRequired("ELASTICSEARCH_USERNAME").flatMap(readPassword),
    readRequired("ELASTICSEARCH_PASSWORD").flatMap(readPassword),
    readRequired("HYGRAPH_URL").flatMap(readURL)
  )(Config.apply)

  def fromEnvironment: IO[ConfigError, Config] = validationFromEnvironment.toZIO

  def layer: TaskLayer[Config] = ZLayer
    .fromZIO(fromEnvironment)
    .mapError(err => new RuntimeException(err.toString))

  def fromConfig(config: Config): ULayer[Config] = ZLayer.succeed(config)
