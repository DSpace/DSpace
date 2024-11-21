# This image will be published as dspace/dspace
# See https://github.com/DSpace/DSpace/tree/main/dspace/src/main/docker for usage details
#
# - note: default tag for branch: dspace/dspace: dspace/dspace:latest

# This Dockerfile uses JDK17 by default.
# To build with other versions, use "--build-arg JDK_VERSION=[value]"
ARG JDK_VERSION=17
ARG DSPACE_VERSION=latest

# Step 1 - Run Maven Build
# UMD Customization
FROM docker.lib.umd.edu/drum-dependencies-8_x:${DSPACE_VERSION} as build
# End UMD Customization
ARG TARGET_DIR=dspace-installer
WORKDIR /app
# The dspace-installer directory will be written to /install
RUN mkdir /install \
    && chown -Rv dspace: /install \
    && chown -Rv dspace: /app
USER dspace
# Copy the DSpace source code (from local machine) into the workdir (excluding .dockerignore contents)
ADD --chown=dspace . /app/
# Build DSpace
# Copy the dspace-installer directory to /install.  Clean up the build to keep the docker image small
# Maven flags here ensure that we skip building test environment and skip all code verification checks.
# These flags speed up this compilation as much as reasonably possible.
ENV MAVEN_FLAGS="-P-test-environment -Denforcer.skip=true -Dcheckstyle.skip=true -Dlicense.skip=true -Dxml.skip=true"
RUN mvn --no-transfer-progress package ${MAVEN_FLAGS} && \
  mv /app/dspace/target/${TARGET_DIR}/* /install && \
  mvn clean
# UMD Customization
# Remove the server webapp to keep image small.
# RUN rm -rf /install/webapps/server/
# End UMD Customization

# Step 2 - Run Ant Deploy
FROM eclipse-temurin:${JDK_VERSION} as ant_build
ARG TARGET_DIR=dspace-installer
# COPY the /install directory from 'build' container to /dspace-src in this container
COPY --from=build /install /dspace-src
WORKDIR /dspace-src
# Create the initial install deployment using ANT
ENV ANT_VERSION 1.10.13
ENV ANT_HOME /tmp/ant-$ANT_VERSION
ENV PATH $ANT_HOME/bin:$PATH
# Need wget to install ant
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && apt-get purge -y --auto-remove \
    && rm -rf /var/lib/apt/lists/*
# Download and install 'ant'
RUN mkdir $ANT_HOME && \
    wget -qO- "https://archive.apache.org/dist/ant/binaries/apache-ant-$ANT_VERSION-bin.tar.gz" | tar -zx --strip-components=1 -C $ANT_HOME
# Run necessary 'ant' deploy scripts
RUN ant init_installation update_configs update_code update_webapps

# UMD Customization
# Step 3 - Run tomcat
# Create a new tomcat image that does not retain the the build directory contents
FROM tomcat:10.1-jdk${JDK_VERSION}
ENV DSPACE_INSTALL=/dspace
ENV TOMCAT_INSTALL=/usr/local/tomcat
# Copy the /dspace directory from 'ant_build' container to /dspace in this container
COPY --from=ant_build /dspace $DSPACE_INSTALL
# Enable the AJP connector in Tomcat's server.xml
# NOTE: secretRequired="false" should only be used when AJP is NOT accessible from an external network. But, secretRequired="true" isn't supported by mod_proxy_ajp until Apache 2.5
RUN sed -i '/Service name="Catalina".*/a \\n    <Connector protocol="AJP/1.3" port="8009" address="0.0.0.0" redirectPort="8443" URIEncoding="UTF-8" secretRequired="false" />' $TOMCAT_INSTALL/conf/server.xml
# Expose Tomcat port and debug port
EXPOSE 8080 8000
# Give java extra memory (2GB)
ENV JAVA_OPTS=-Xmx2000m

ENV TZ=America/New_York

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y \
        rsync \
        cron \
        csh \
        postfix \
        s-nail \
        libgetopt-complete-perl \
        libconfig-properties-perl \
        vim \
        python3-lxml && \
    mkfifo /var/spool/postfix/public/pickup && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Link the DSpace 'server' webapp into Tomcat's webapps directory.
# This ensures that when we start Tomcat, it runs from /server path (e.g. http://localhost:8080/server/)
# Also link the v6.x (deprecated) REST API off the "/rest" path
RUN ln -s $DSPACE_INSTALL/webapps/server   /usr/local/tomcat/webapps/server
# If you wish to run "server" webapp off the ROOT path, then comment out the above RUN, and uncomment the below RUN.
# You also MUST update the 'dspace.server.url' configuration to match.
# Please note that server webapp should only run on one path at a time.
#RUN mv /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/webapps/ROOT.bk && \
#    ln -s $DSPACE_INSTALL/webapps/server   /usr/local/tomcat/webapps/ROOT
# End UMD Customization
