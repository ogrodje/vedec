import io.circe.Json

type DocumentID     = String
type Document       = Seq[(String, Json)]
type DocumentWithID = (Option[DocumentID], Document)
type DocumentValue  = Json

object DocumentValue:
  given Conversion[String, DocumentValue]  = Json.fromString
  given Conversion[Int, DocumentValue]     = Json.fromInt
  given Conversion[Boolean, DocumentValue] = Json.fromBoolean
