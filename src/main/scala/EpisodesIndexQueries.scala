import io.circe.Json
object EpisodesIndexQueries:
  import DocumentValue.given

  private val returnedFields: Json = Json.arr(
    List(
      "id",
      "name",
      "code",
      "summary",
      "machineSummary",
      "airedAt",
      "show.name",
      "topics.name",
      "supporters",
      "host.fullName",
      "cohosts.fullName",
      "guests.fullName",
      "multimediaProducers.fullName",
      "consultants.fullName",
      "designers.fullName"
    ).map(Json.fromString)*
  )

  def query(query: String): Seq[(String, Json)] = Seq(
    "_source"   -> returnedFields,
    "query"     -> Json.obj(
      "bool" -> Json.obj(
        "minimum_should_match" -> 1,
        "should"               -> Json.arr(
          Json.obj(
            "multi_match" -> Json.obj(
              "query"  -> query,
              "fields" -> Json.arr(
                "name",
                "summary",
                "machineSummary",
                "code",
                "show.name",
                "topics.name",
                "host.fullName",
                "cohosts.fullName",
                "guests.fullName",
                "multimediaProducers.fullName",
                "designers.fullName"
              )
            )
          ),
          Json.obj(
            "match"       -> Json.obj(
              "guests.fullName" -> Json.obj(
                "query"     -> query,
                "boost"     -> 3,
                "fuzziness" -> "AUTO"
              )
            )
          ),
          Json.obj(
            "match"       -> Json.obj(
              "name" -> Json.obj(
                "query"     -> query,
                "boost"     -> 4,
                "fuzziness" -> "AUTO"
              )
            )
          )
        )
      )
    ),
    "highlight" -> Json.obj(
      "fields" -> Json.obj(
        "name"           -> Json.obj(),
        "summary"        -> Json.obj(),
        "machineSummary" -> Json.obj()
      )
    ),
    "aggs"      -> Json.obj(
      "shows"  -> Json.obj(
        "terms" -> Json.obj(
          "field" -> "show.name.keyword",
          "size"  -> 10
        )
      ),
      "topics" -> Json.obj(
        "terms" -> Json.obj(
          "field" -> "topics.name.keyword",
          "size"  -> 10
        )
      ),
      "codes"  -> Json.obj(
        "terms" -> Json.obj(
          "field" -> "code",
          "size"  -> 10
        )
      )
    )
  )
