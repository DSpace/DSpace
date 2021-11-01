# This image will be published as dspace/dspace
# See https://dspace-labs.github.io/DSpace-Docker-Images/ for usage details
#
# This version is JDK8 compatible
# - tomcat:8-jre8
# - ANT 1.10.7
# - maven:3-jdk-8
# - note:
# - default tag for branch: dspace/dspace: dspace/dspace:dspace-6_x-jdk8

# Step 1 - Run Maven Build
FROM docker.lib.umd.edu/drum-dependencies-6_x:latest as build
ARG TARGET_DIR=dspace-installer
WORKDIR /app

# The dspace-install directory will be written to /install
RUN mkdir /install \
    && chown -Rv dspace: /install

USER dspace

# Copy the DSpace source code into the workdir (excluding .dockerignore contents)
ADD --chown=dspace . /app/
COPY dspace/src/main/docker/local.cfg /app/local.cfg

# Build DSpace.  Copy the dspace-install directory to /install.  Clean up the build to keep the docker image small
RUN mvn package -Denv=local -P \!dspace-jspui,\!dspace-lni,\!dspace-rdf,\!dspace-solr,\!dspace-sword,\!dspace-swordv2 && \
  mv /app/dspace/target/${TARGET_DIR}/* /install && \
  mvn clean

# Step 2 - Run Ant Deploy
FROM tomcat:8-jre8 as ant_build
ARG TARGET_DIR=dspace-installer
COPY --from=build /install /dspace-src
WORKDIR /dspace-src

# Create the initial install deployment using ANT
ENV ANT_VERSION 1.10.7
ENV ANT_HOME /tmp/ant-$ANT_VERSION
ENV PATH $ANT_HOME/bin:$PATH
ENV TEST=
RUN mkdir $ANT_HOME && \
    wget -qO- "https://archive.apache.org/dist/ant/binaries/apache-ant-$ANT_VERSION-bin.tar.gz" | tar -zx --strip-components=1 -C $ANT_HOME

RUN ant init_installation update_configs update_code update_webapps update_solr_indexes

# Step 3 - Run tomcat
# Create a new tomcat image that does not retain the the build directory contents
FROM tomcat:8-jre8

ENV DSPACE_INSTALL=/dspace \
    JAVA_OPTS=-Xmx2000m \
    TZ=America/New_York

COPY --from=ant_build /dspace $DSPACE_INSTALL
EXPOSE 8080 8009

RUN apt-get update && \
    apt-get install -y \
        rsync \
        cron \
        postfix \
        vim \
        python3-lxml && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    rm -rf /usr/local/tomcat/webapps/* && \
    ln -s $DSPACE_INSTALL/webapps/xmlui /usr/local/tomcat/webapps/ROOT && \
    ln -s $DSPACE_INSTALL/webapps/rest /usr/local/tomcat/webapps/rest && \
    ln -s $DSPACE_INSTALL/webapps/oai /usr/local/tomcat/webapps/oai && \
    ln -s $DSPACE_INSTALL/webapps/rdf /usr/local/tomcat/webapps/rdf
