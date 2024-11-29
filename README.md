# vedec

```bash
sbt Docker / publishLocal
docker run -ti --rm ghcr.io/ogrodje/vedec server
docker run -ti --rm ghcr.io/ogrodje/vedec index


curl -sS -XGET 'Content-Type: application/json' http://localhost:4442/search -d '{}' | jq . | more
```