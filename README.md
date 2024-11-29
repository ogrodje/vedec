# vedec

Vedec is ElasticSearch-based indexer and search engine for [Ogrodje](https://ogrodje.si).

```bash
sbt Docker / publishLocal

docker run -ti --rm ghcr.io/ogrodje/vedec server
docker run -ti --rm ghcr.io/ogrodje/vedec index


curl -sS -XGET 'Content-Type: application/json' http://localhost:4442/search -d '{}' | jq . | more
```

## Author

- [Oto Brglez](https://github.com/otobrglez)
