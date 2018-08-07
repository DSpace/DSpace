# This image will be published as dspace/dspace
# See https://dspace-labs.github.io/DSpace-Docker-Images/ for usage details

# Step 1 - Run Maven Build
FROM maven as build
WORKDIR /app

# The Mirage2 build cannot run as root.  Setting the user to dspace.
RUN useradd dspace \
    && mkdir /home/dspace \
    && chown -Rv dspace: /home/dspace
USER dspace

# Copy the DSpace source code into the workdir (excluding .dockerignore contents)
ADD --chown=dspace . /app/
COPY dspace/src/main/docker/local.cfg /app/local.cfg

RUN mvn package -Dmirage2.on=true

# Step 2 - Run Ant Deploy
FROM tomcat:8 as ant_build
ARG TARGET_DIR=dspace-installer
COPY --from=build /app /dspace-src
WORKDIR /dspace-src/dspace/target/${TARGET_DIR}

# Create the initial install deployment using ANT
ENV ANT_VERSION 1.10.5
ENV ANT_HOME /tmp/ant-$ANT_VERSION
ENV PATH $ANT_HOME/bin:$PATH

RUN mkdir $ANT_HOME && \
    wget -qO- "https://www.apache.org/dist/ant/binaries/apache-ant-$ANT_VERSION-bin.tar.gz" | tar -zx --strip-components=1 -C $ANT_HOME

RUN ant update_configs update_code update_webapps update_solr_indexes

# Step 3 - Run tomcat
# Create a new tomcat image that does not retain the the build directory contents
FROM tomcat:8
COPY --from=ant_build /dspace /dspace
EXPOSE 8080 8009

# Ant will be embedded in the final container to allow additional deployments
ENV ANT_VERSION 1.10.5
ENV ANT_HOME /tmp/ant-$ANT_VERSION
ENV PATH $ANT_HOME/bin:$PATH

RUN mkdir $ANT_HOME && \
    wget -qO- "https://www.apache.org/dist/ant/binaries/apache-ant-$ANT_VERSION-bin.tar.gz" | tar -zx --strip-components=1 -C $ANT_HOME

ENV DSPACE_INSTALL=/dspace
ENV JAVA_OPTS=-Xmx2000m

RUN ln -s $DSPACE_INSTALL/webapps/solr    /usr/local/tomcat/webapps/solr    && \
    ln -s $DSPACE_INSTALL/webapps/xmlui   /usr/local/tomcat/webapps/xmlui   && \
    ln -s $DSPACE_INSTALL/webapps/jspui   /usr/local/tomcat/webapps/jspui   && \
    ln -s $DSPACE_INSTALL/webapps/rest    /usr/local/tomcat/webapps/rest    && \
    ln -s $DSPACE_INSTALL/webapps/oai     /usr/local/tomcat/webapps/oai     && \
    ln -s $DSPACE_INSTALL/webapps/rdf     /usr/local/tomcat/webapps/rdf     && \
    ln -s $DSPACE_INSTALL/webapps/sword   /usr/local/tomcat/webapps/sword   && \
    ln -s $DSPACE_INSTALL/webapps/swordv2 /usr/local/tomcat/webapps/swordv2
