#!/bin/bash
set -e

# Replace the ${APACHE_SERVER_NAME} placeholder in our shibboleth2.xml with the current value of the
# $APACHE_SERVER_NAME environment variable.
sed -i "s/\${APACHE_SERVER_NAME}/$APACHE_SERVER_NAME/g" /etc/shibboleth/shibboleth2.xml

# Start the Shibboleth daemon
/etc/init.d/shibd start

# Remove existing Apache PID files (if any)
rm -f /var/run/apache2/apache2.pid

# Start Apache (in foreground)
exec /usr/sbin/apache2ctl -DFOREGROUND
