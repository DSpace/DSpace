# This image will be published as dspace/dspace
# See https://github.com/DSpace/DSpace/tree/main/dspace/src/main/docker for usage details
#
# - note: default tag for branch: dspace/dspace: dspace/dspace:dspace-7_x

# This Dockerfile uses JDK11 by default, but has also been tested with JDK17.
# To build with JDK17, use "--build-arg JDK_VERSION=17"
ARG JDK_VERSION=11

# Step 1 - Run Maven Build
FROM dspace/dspace-dependencies:dspace-7_x as build
ARG TARGET_DIR=dspace-installer
WORKDIR /app
# The dspace-installer directory will be written to /install
RUN mkdir /install \
    && chown -Rv dspace: /install \
    && chown -Rv dspace: /app
USER dspace
# Copy the DSpace source code (from local machine) into the workdir (excluding .dockerignore contents)
ADD --chown=dspace . /app/
# Build DSpace (note: this build doesn't include the optional, deprecated "dspace-rest" webapp)
# Copy the dspace-installer directory to /install.  Clean up the build to keep the docker image small
RUN mvn --no-transfer-progress package && \
  mv /app/dspace/target/${TARGET_DIR}/* /install && \
  mvn clean

# Step 2 - Run Ant Deploy
FROM openjdk:${JDK_VERSION}-slim as ant_build
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

# Step 3 - Run tomcat
# Create a new tomcat image that does not retain the the build directory contents
FROM tomcat:9-jdk${JDK_VERSION}
# NOTE: DSPACE_INSTALL must align with the "dspace.dir" default configuration.
ENV DSPACE_INSTALL=/dspace
# Copy the /dspace directory from 'ant_build' container to /dspace in this container
COPY --from=ant_build /dspace $DSPACE_INSTALL
# Expose Tomcat port and AJP port
EXPOSE 8080 8009
# Give java extra memory (2GB)
ENV JAVA_OPTS=-Xmx2000m

# Link the DSpace 'server' webapp into Tomcat's webapps directory.
# This ensures that when we start Tomcat, it runs from /server path (e.g. http://localhost:8080/server/)
RUN ln -s $DSPACE_INSTALL/webapps/server   /usr/local/tomcat/webapps/server
# If you wish to run "server" webapp off the ROOT path, then comment out the above RUN, and uncomment the below RUN.
# You also MUST update the 'dspace.server.url' configuration to match.
# Please note that server webapp should only run on one path at a time.
#RUN mv /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/webapps/ROOT.bk && \
#    ln -s $DSPACE_INSTALL/webapps/server   /usr/local/tomcat/webapps/ROOT
