#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

FROM solr:latest
EXPOSE 8983 8983

WORKDIR /opt/solr/server/solr
USER solr
RUN \
    cp -r configsets/_default authority && \
    mkdir authority/data &&\
    cp -r configsets/_default oai && \
    mkdir oai/data &&\
    cp -r configsets/_default search && \
    mkdir search/data &&\
    cp -r configsets/_default statistics && \
    mkdir statistics/data

COPY dspace/solr/authority authority/
COPY dspace/solr/oai oai/
COPY dspace/solr/search search/
COPY dspace/solr/statistics statistics/
