import io.circe.Json
import zio.prelude.NonEmptyList
import zio.{Task, ZIO, ZLayer}
import ZIO.{fromEither, fromOption}

import scala.util.control.NoStackTrace

type EpisodeJSON  = Json
type EpisodesJSON = NonEmptyList[EpisodeJSON]

final case class NoEpisodes(message: String) extends RuntimeException(message) with NoStackTrace

final class EpisodesGraph private (graph: HyGraph):
  private val personFragment =
    """fragment PersonF on Person {
      | id
      | fullName
      |}""".stripMargin

  private val commonFields =
    """topics { name }
      |stage
      |show { id, name, color }
      |summary
      |machineSummary
      |name
      |id
      |code
      |airedAt
      |host { ...PersonF }
      |cohosts { ...PersonF }
      |guests { ... PersonF }
      |multimediaProducers { ...PersonF }
      |consultants { ...PersonF }
      |designers { ...PersonF }
      |supporters(orderBy: name_ASC) {
      |  id, name, bio
      |}
      |anchorUrl
      |applePodcastsUrl
      |castboxUrl
      |youTubeUrl
      |spotifyUrl
      |""".stripMargin

  private val decodeEpisodes: Json => Task[EpisodesJSON] =
    json =>
      fromEither(json.hcursor.downField("episodes").as[List[EpisodeJSON]])
        .flatMap(list =>
          fromOption(NonEmptyList.fromIterableOption(list))
            .orElseFail(NoEpisodes("Received no episodes from Graph"))
        )

  def episodes(size: Int = 200): Task[EpisodesJSON] =
    graph
      .query(
        s"""query PublishedEpisodes($$size: Int, $$stage: Stage!) {
           |  episodes(first: $$size, orderBy: airedAt_DESC, stage: $$stage) {
           |    $commonFields
           |  }
           |}
           |$personFragment""".stripMargin,
        "size"  -> Json.fromInt(size),
        "stage" -> Json.fromString("PUBLISHED")
      )
      .flatMap(decodeEpisodes)

object EpisodesGraph:
  def layer = ZLayer.derive[EpisodesGraph]
