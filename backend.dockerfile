# Step 1 - Run Maven Build
FROM maven:3.9-eclipse-temurin-17 AS mvn_build
ARG TARGET_DIR=dspace-installer
WORKDIR /app
# Create the 'dspace' user account & home directory
RUN useradd dspace \
    && mkdir -p /home/dspace \
    && chown -Rv dspace: /home/dspace
RUN chown -Rv dspace: /app
# Need git to support buildnumber-maven-plugin, which lets us know what version of DSpace is being run.
RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && apt-get purge -y --auto-remove \
    && rm -rf /var/lib/apt/lists/*

# Copy the DSpace source code (from local machine) into the workdir (excluding .dockerignore contents)
ADD --chown=dspace . /app/

# The dspace-installer directory will be written to /install
RUN mkdir /install && chown -Rv dspace: /install

# Switch to dspace user & run below commands as that user
USER dspace

# Build DSpace (INCLUDING the optional, deprecated "dspace-rest" webapp)
# Copy the dspace-installer directory to /install.  Clean up the build to keep the docker image small
RUN mvn --no-transfer-progress package -Pdspace-rest && \
  mv /app/dspace/target/${TARGET_DIR}/* /install && \
  mvn clean

# Step 2 - Run Ant Deploy
FROM eclipse-temurin:17 AS ant_build
ARG TARGET_DIR=dspace-installer
# COPY the /install directory from 'build' container to /dspace-src in this container
COPY --from=mvn_build /install /dspace-src
WORKDIR /dspace-src
# Create the initial install deployment using ANT
ENV ANT_VERSION=1.10.12
ENV ANT_HOME=/tmp/ant-$ANT_VERSION
ENV PATH=$ANT_HOME/bin:$PATH
# Need wget to install ant; download and install ant, then purge wget
RUN apt-get -o Acquire::Retries=3 update \
    && apt-get -o Acquire::Retries=3 install -y --no-install-recommends wget \
    && mkdir $ANT_HOME \
    && wget -qO- "https://archive.apache.org/dist/ant/binaries/apache-ant-$ANT_VERSION-bin.tar.gz" | tar -zx --strip-components=1 -C $ANT_HOME \
    && apt-get purge -y --auto-remove wget \
    && rm -rf /var/lib/apt/lists/*
# Run necessary 'ant' deploy scripts
RUN ant init_installation update_configs update_code update_webapps

# Step 3 - Run tomcat
# Create a new tomcat image that does not retain the the build directory contents
FROM tomcat:9-jdk17
ENV DSPACE_INSTALL=/dspace
ENV TOMCAT_INSTALL=/usr/local/tomcat
# Copy the /dspace directory from 'ant_build' containger to /dspace in this container
COPY --from=ant_build /dspace $DSPACE_INSTALL

# Install additional libraries needed for backend scripts
RUN apt-get -o Acquire::Retries=3 update \
    && apt-get -o Acquire::Retries=3 upgrade -y \
    && apt-get -o Acquire::Retries=3 install -y --no-install-recommends \
        libcgi-pm-perl \
        libdbi-perl \
        libio-all-lwp-perl \
        liberror-perl \
        libdbd-pg-perl \
        libjson-xs-perl \
        libjson-perl \
        libemail-sender-perl \
        libemail-mime-perl \
        libemail-stuffer-perl \
        libmime-lite-perl \
        libnet-sftp-foreign-perl \
        libmailtools-perl \
        libtext-csv-perl \
        libio-pty-perl \
        unzip \
        zip \
        xsltproc \
        dnsutils \
        emacs \
        vim \
        build-essential  \
        ruby-dev \
        pipx \
        iputils-ping \
        mailutils \
        curl \
    && rm -rf /var/lib/apt/lists/*

RUN gem install uri pry net-http json
RUN pipx install awscli
RUN ln -s /mnt/assetstore/dspace/repository/dev-test/asset_jose /dspace/assetstore

RUN mkdir /root/.emacs.d
# Install additional backend scripts
COPY ./backend/init.el /root/.emacs.d/init.el
COPY ./backend/bin/ $DSPACE_INSTALL/bin/

# The logs directory is already created by `ant init_installation` above,
# so the explicit mkdir is redundant. Kept here (commented out) as a reminder
# in case the ant install layout ever changes.
# RUN mkdir -p $DSPACE_INSTALL/logs

# Enable the AJP connector in Tomcat's server.xml
# NOTE: secretRequired="false" should only be used when AJP is NOT accessible from an external network. But, secretRequired="true" isn't supported by mod_proxy_ajp until Apache 2.5
RUN sed -i '/Service name="Catalina".*/a \\n    <Connector protocol="AJP/1.3" port="8009" address="0.0.0.0" redirectPort="8443" URIEncoding="UTF-8" secretRequired="false" maxHttpRequestHeaderSize="262144" maxHttpHeaderSize="16384" />' $TOMCAT_INSTALL/conf/server.xml

RUN sed -i '/<Valve className="org.apache.catalina.valves.AccessLogValve/,/\/>/d' $TOMCAT_INSTALL/conf/server.xml

# Expose Tomcat port and AJP port
EXPOSE 8080 8009
# Give java extra memory (minimum 2GB; currently set to 10GB for production workloads)
ENV JAVA_OPTS=-Xmx10g

# Link the DSpace 'server' webapp into Tomcat's webapps directory.
# This ensures that when we start Tomcat, it runs from /server path (e.g. http://localhost:8080/server/)
# Also link the v6.x (deprecated) REST API off the "/rest" path
RUN ln -s $DSPACE_INSTALL/webapps/server   /usr/local/tomcat/webapps/server   && \
    ln -s $DSPACE_INSTALL/webapps/rest          /usr/local/tomcat/webapps/rest
# If you wish to run "server" webapp off the ROOT path, then comment out the above RUN, and uncomment the below RUN.
# You also MUST update the 'dspace.server.url' configuration to match.
# Please note that server webapp should only run on one path at a time.
#RUN mv /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/webapps/ROOT.bk && \
#    ln -s $DSPACE_INSTALL/webapps/server   /usr/local/tomcat/webapps/ROOT && \
#    ln -s $DSPACE_INSTALL/webapps/rest          /usr/local/tomcat/webapps/rest

# Overwrite the v6.x (deprecated) REST API's web.xml, so that we can run it on HTTP (defaults to requiring HTTPS)
# WARNING: THIS IS OBVIOUSLY INSECURE. NEVER DO THIS IN PRODUCTION.
COPY --from=source /DSpace/dspace/src/main/docker/test/rest_web.xml $DSPACE_INSTALL/webapps/rest/WEB-INF/web.xml
RUN sed -i -e "s|\${dspace.dir}|$DSPACE_INSTALL|" $DSPACE_INSTALL/webapps/rest/WEB-INF/web.xml
