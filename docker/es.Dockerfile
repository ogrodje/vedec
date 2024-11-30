FROM bitnami/elasticsearch:8.16.0

ADD docker/elasticsearch/config/hunspell /opt/bitnami/elasticsearch/config/hunspell
ADD docker/elasticsearch/config/lemmagen /opt/bitnami/elasticsearch/config/lemmagen

ADD docker/elasticsearch-analysis-lemmagen-8.16.0-plugin.zip /tmp/elasticsearch-analysis-lemmagen-plugin.zip

RUN ./opt/bitnami/elasticsearch/bin/elasticsearch-plugin \
    install file:///tmp/elasticsearch-analysis-lemmagen-plugin.zip