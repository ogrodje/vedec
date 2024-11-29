FROM bitnami/elasticsearch:latest

ADD docker/elasticsearch/config/hunspell /opt/bitnami/elasticsearch/config/hunspell
