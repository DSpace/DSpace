#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

# Define a 'dspace' alias for our latest v7 image
FROM dspace/dspace:dspace-7_x-jdk8 as dspace

# Pin to Solr v7 (v8 currently has build issues)
FROM solr:7
# Directory on 'dspace' image (see above) where DSpace is installed
ENV DSPACE_INSTALL=/dspace
# Expose Solr on localhost:8983
EXPOSE 8983 8983

WORKDIR /opt/solr/server/solr
USER solr
# Create DSpace-specific Solr cores (based on default configset provided by Solr)
RUN \
    cp -r configsets/_default authority && \
    mkdir authority/data &&\
    cp -r configsets/_default oai && \
    mkdir oai/data &&\
    cp -r configsets/_default search && \
    mkdir search/data &&\
    cp -r configsets/_default statistics && \
    mkdir statistics/data

# Copy the DSpace-specific Solr schemas & configs (from our 'dspace' image)
# into corresponding Solr core directory
COPY --from=dspace $DSPACE_INSTALL/solr/authority authority/
COPY --from=dspace $DSPACE_INSTALL/solr/oai oai/
COPY --from=dspace $DSPACE_INSTALL/solr/search search/
COPY --from=dspace $DSPACE_INSTALL/solr/statistics statistics/
