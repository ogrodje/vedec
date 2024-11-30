# vedec

Vedec is an [ElasticSearch](https://www.elastic.co/elasticsearch)-based indexer and search engine
for [Ogrodje](https://ogrodje.si).

The engine is hacked and fine-tuned to work with the Slovenian language.

It uses the [Hunspell token filter](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-hunspell-tokenfilter.html)
and [hunspell](https://hunspell.github.io/) - spell checker and morphological analyser.

Additional language support is provided via custom [elasticsearch-analysis-lemmagen](https://github.com/vhyza/elasticsearch-analysis-lemmagen) build with `jLemmaGen`.


## Development & compilation

```bash
sbt Docker / publishLocal
./bin/og-dev.sh build

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
