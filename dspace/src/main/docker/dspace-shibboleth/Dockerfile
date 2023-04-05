#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

# This will be deployed as dspace/dspace-shibboleth:latest
# Build from Ubuntu as it has easy Apache tooling (e.g. a2enmod script is debian only).
# Apache & mod_shib are required for DSpace to act as an SP
# See also https://wiki.lyrasis.org/display/DSDOC7x/Authentication+Plugins#AuthenticationPlugins-ShibbolethAuthentication
FROM ubuntu:20.04

# Apache ENVs (default values)
ENV APACHE_RUN_USER www-data
ENV APACHE_RUN_GROUP www-data
ENV APACHE_LOCK_DIR /var/lock/apache2
ENV APACHE_LOG_DIR /var/log/apache2
ENV APACHE_PID_FILE /var/run/apache2/apache2.pid
ENV APACHE_SERVER_NAME localhost

# Ensure Apache2, mod_shib & shibboleth daemon are installed.
# Also install ssl-cert to provide a local SSL cert for use with Apache
RUN apt-get update \
      && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
           apache2 \
           ca-certificates \
           ssl-cert \
           libapache2-mod-shib2 \
      && apt-get clean \
      && rm -rf /var/lib/apt/lists/* \
      && rm -rf /tmp/*

# Setup Apache configs & enable mod-shib & dspace vhost
COPY dspace-vhost.conf /etc/apache2/sites-available/
RUN a2enmod ssl proxy proxy_ajp proxy_http shib && \
    a2dissite 000-default.conf default-ssl.conf && \
    a2ensite dspace-vhost.conf

# Setup Shibboleth configs
COPY shibboleth2.xml /etc/shibboleth/
COPY attribute-map.xml /etc/shibboleth/
RUN cd /etc/shibboleth/ \
    && shib-keygen

# Copy over our startup script
COPY httpd-shibd-foreground.sh /usr/local/bin/

# Expose HTTP and HTTPS ports for Apache
EXPOSE 80 443

# Launch Shib & Apache
CMD ["httpd-shibd-foreground.sh"]
