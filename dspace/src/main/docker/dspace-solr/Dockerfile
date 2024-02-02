#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

# To build use root as context for (easier) access to solr cfgs
# docker build --build-arg SOLR_VERSION=8.11 -f ./dspace/src/main/docker/dspace-solr/Dockerfile .
# This will be published as dspace/dspace-solr:$DSPACE_VERSION

ARG SOLR_VERSION=8.11

FROM solr:${SOLR_VERSION}-slim

ENV AUTHORITY_CONFIGSET_PATH=/opt/solr/server/solr/configsets/authority/conf \
    OAI_CONFIGSET_PATH=/opt/solr/server/solr/configsets/oai/conf \
    SEARCH_CONFIGSET_PATH=/opt/solr/server/solr/configsets/search/conf \
    STATISTICS_CONFIGSET_PATH=/opt/solr/server/solr/configsets/statistics/conf

USER root

RUN mkdir -p $AUTHORITY_CONFIGSET_PATH && \
    mkdir -p $OAI_CONFIGSET_PATH && \
    mkdir -p $SEARCH_CONFIGSET_PATH && \
    mkdir -p $STATISTICS_CONFIGSET_PATH

# NOTE: "solrconfigs" MUST be passed in by docker-compose via "additional_contexts"
# OR via "docker build --build-context solrconfigs=[path-to-dspace/solr]"
COPY --from=solrconfigs authority/conf/* $AUTHORITY_CONFIGSET_PATH/
COPY --from=solrconfigs oai/conf/* $OAI_CONFIGSET_PATH/
COPY --from=solrconfigs search/conf/* $SEARCH_CONFIGSET_PATH/
COPY --from=solrconfigs statistics/conf/* $STATISTICS_CONFIGSET_PATH/

RUN chown -R solr:solr /opt/solr/server/solr/configsets

USER solr
