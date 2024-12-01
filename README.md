# vedec

Vedec is an [ElasticSearch](https://www.elastic.co/elasticsearch)-based indexer and search engine
for [Ogrodje](https://ogrodje.si).

The engine is hacked and fine-tuned to work with the Slovenian language.

It uses the [Hunspell token filter](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-hunspell-tokenfilter.html)
and [hunspell](https://hunspell.github.io/) - spell checker and morphological analyser.

Additional Slovenian language support is provided via custom [elasticsearch-analysis-lemmagen](https://github.com/vhyza/elasticsearch-analysis-lemmagen) built with `jLemmaGen`.

**The indexer** is tuned to extract data from [HyGraph](https://hygraph.com/) - [GraphQL](https://graphql.org/)-based [Headless CMS](https://en.wikipedia.org/wiki/Headless_content_management_system). The [`EpisodesIndexDefinition.scala`](src/main/scala/EpisodesIndexDefinition.scala) keeps the index configuration and mapping, and the search query is defined in [`EpisodesIndexQueries.scala`](src/main/scala/EpisodesIndexQueries.scala)

## Development & compilation

```bash
sbt Docker / publishLocal
./bin/og-dev.sh build
./bin/og-dev.sh up # all services

# individual (ElasticSearch needs to be booted)
docker run -ti --rm ghcr.io/ogrodje/vedec server
docker run -ti --rm ghcr.io/ogrodje/vedec index

curl -sS -XGET 'Content-Type: application/json' http://localhost:4442/search -d '{}' | jq . | more
```

## API Endpoints

```bash
curl -sS -XGET 'Content-Type: application/json' http://localhost:4442/query\?query\=izzivi | jq .
```

## Author

- [Oto Brglez](https://github.com/otobrglez)
