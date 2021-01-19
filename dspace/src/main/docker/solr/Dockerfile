#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

# Define a 'dspace' alias for our latest v7 image
FROM dspace/dspace:dspace-7_x as dspace

# Pin to Solr v8.x
FROM solr:8
# Directory on 'dspace' image (see above) where DSpace is installed
ENV DSPACE_INSTALL=/dspace
# User that Solr runs as
ENV SOLR_USER=solr
# Expose Solr on localhost:8983
EXPOSE 8983 8983

# Solr Data Directory (NOTE: logs are in /var/solr/logs)
WORKDIR /var/solr/data
USER $SOLR_USER
# Create DSpace-specific Solr cores (under our WORKDIR)
RUN \
    init-var-solr && \
    precreate-core authority && \
    precreate-core oai && \
    precreate-core search && \
    precreate-core statistics

# Copy the DSpace-specific Solr schemas & configs (from our 'dspace' image)
# into corresponding Solr core directory
COPY --from=dspace --chown=$SOLR_USER:$SOLR_USER $DSPACE_INSTALL/solr/authority authority/
COPY --from=dspace --chown=$SOLR_USER:$SOLR_USER $DSPACE_INSTALL/solr/oai oai/
COPY --from=dspace --chown=$SOLR_USER:$SOLR_USER $DSPACE_INSTALL/solr/search search/
COPY --from=dspace --chown=$SOLR_USER:$SOLR_USER $DSPACE_INSTALL/solr/statistics statistics/
