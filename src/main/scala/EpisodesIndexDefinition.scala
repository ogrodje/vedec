import io.circe.Json

object EpisodesIndexDefinition:
  import DocumentValue.given
  val definition: Seq[(String, Json)] = Seq(
    "mappings" -> Json.obj(
      "properties" -> Json.obj(
        "name"                -> Json.obj(
          "type"     -> "text",
          "analyzer" -> "sl_SI"
        ),
        "summary"             -> Json.obj(
          "type"     -> "text",
          "analyzer" -> "lemmagen_lowercase_sl"
        ),
        "machineSummary"      -> Json.obj(
          "type"     -> "text",
          // "analyzer" -> "sl_SI"
          "analyzer" -> "lemmagen_lowercase_sl"
        ),
        "code"                -> Json.obj(
          "type" -> "keyword"
        ),
        "airedAt"             -> Json.obj(
          "type"   -> "date",
          "format" -> "yyyy-MM-dd"
        ),
        "show"                -> Json.obj(
          "type"       -> "object",
          "properties" -> Json.obj(
            "name" -> Json.obj(
              "type"     -> "text",
              "analyzer" -> "standard",
              "fields"   -> Json.obj(
                "keyword" -> Json.obj("type" -> "keyword")
              )
            )
          )
        ),
        "topics"              -> Json.obj(
          "type"       -> "nested",
          "properties" -> Json.obj(
            "name" -> Json.obj(
              "type"     -> "text",
              "analyzer" -> "sl_SI",
              "fields"   -> Json.obj(
                "keyword" -> Json.obj("type" -> "keyword")
              )
            )
          )
        ),
        "supporters"          -> Json.obj(
          "type"       -> "object",
          "properties" -> Json.obj(
            "name" -> Json.obj(
              "type" -> "keyword"
            )
          )
        ),
        "host"                -> Json.obj(
          "type"       -> "object",
          "properties" -> Json.obj(
            "fullName" -> Json.obj(
              "type" -> "keyword"
            )
          )
        ),
        "cohosts"             -> Json.obj(
          "type"       -> "nested",
          "properties" -> Json.obj(
            "fullName" -> Json.obj(
              "type" -> "keyword"
            )
          )
        ),
        "guests"              -> Json.obj(
          "type"       -> "nested",
          "properties" -> Json.obj(
            "fullName" -> Json.obj(
              "type" -> "keyword"
            )
          )
        ),
        "multimediaProducers" -> Json.obj(
          "type"       -> "nested",
          "properties" -> Json.obj(
            "fullName" -> Json.obj(
              "type" -> "keyword"
            )
          )
        ),
        "consultants"         -> Json.obj(
          "type"       -> "nested",
          "properties" -> Json.obj(
            "fullName" -> Json.obj(
              "type" -> "keyword"
            )
          )
        ),
        "designers"           -> Json.obj(
          "type"       -> "nested",
          "properties" -> Json.obj(
            "fullName" -> Json.obj(
              "type" -> "keyword"
            )
          )
        )
      )
    ),
    "settings" -> Json.obj(
      "number_of_shards"   -> 1,
      "number_of_replicas" -> 0,
      "analysis"           -> Json.obj(
        "analyzer" -> Json.obj(
          "sl_SI"                 -> Json.obj(
            "tokenizer" -> "standard",
            "filter"    -> Json.arr(Json.fromString("lowercase"), Json.fromString("sl_SI"))
          ),
          "lemmagen_sl"           -> Json.obj(
            "tokenizer" -> "standard",
            "filter"    -> Json.arr(Json.fromString("lemmagen_filter_sl"))
          ),
          "lemmagen_lowercase_sl" -> Json.obj(
            "tokenizer" -> "standard",
            "filter"    -> Json.arr(Json.fromString("lemmagen_filter_sl"), Json.fromString("lowercase"))
          )
        ),
        "filter"   -> Json.obj(
          "sl_SI"              -> Json.obj(
            "type"     -> "hunspell",
            "language" -> "sl_SI",
            "dedup"    -> false
          ),
          "lemmagen_filter_sl" -> Json.obj(
            "type"    -> "lemmagen",
            "lexicon" -> "sl"
          )
        )
      )
    )
  )
