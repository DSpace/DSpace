FROM solr:8.11-slim

COPY ./dspace/solr/authority /opt/solr/server/solr/configsets/authority
COPY ./dspace/solr/oai /opt/solr/server/solr/configsets/oai
COPY ./dspace/solr/search /opt/solr/server/solr/configsets/search
COPY ./dspace/solr/statistics /opt/solr/server/solr/configsets/statistics
