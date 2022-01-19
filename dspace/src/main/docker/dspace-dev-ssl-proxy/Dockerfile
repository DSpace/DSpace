#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

FROM httpd:2

# Default environment variables
ENV APACHE_HOME /usr/local/apache2
ENV APACHE_SERVER_NAME localhost

# Install openssl to generate our cert
RUN apt-get update \
	&& apt-get install openssl \
	&& rm -rf /var/lib/apt/lists/*

# Generate our self-signed 'localhost' certificate to /ssl/cert.pem
RUN mkdir -p /ssl
WORKDIR /ssl
RUN openssl req -subj '/CN=localhost' -x509 -newkey rsa:4096 -nodes -keyout key.pem -out cert.pem -days 365

# Enable SSL/HTTPS in Apache
RUN sed -i \
        -e 's/^#\(Include .*httpd-ssl.conf\)/\1/' \
        -e 's/^#\(LoadModule .*mod_ssl.so\)/\1/' \
        -e 's/^#\(LoadModule .*mod_socache_shmcb.so\)/\1/' \
        $APACHE_HOME/conf/httpd.conf

# Enable proxy modules in Apache
RUN sed -i \
        -e 's/^#\(LoadModule .*mod_proxy.so\)/\1/' \
        -e 's/^#\(LoadModule .*mod_proxy_http.so\)/\1/' \
        -e 's/^#\(LoadModule .*mod_proxy_ajp.so\)/\1/' \
        $APACHE_HOME/conf/httpd.conf

# Copy our custom HTTPS VirtualHost config to Apache
COPY ./httpd-ssl.conf  $APACHE_HOME/conf/extra/httpd-ssl.conf
