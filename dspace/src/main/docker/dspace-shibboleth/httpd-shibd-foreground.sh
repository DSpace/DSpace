#!/bin/bash
set -e

# Start the Shibboleth daemon
/etc/init.d/shibd start

# Remove existing Apache PID files (if any)
rm -f /var/run/apache2/apache2.pid

# Start Apache (in foreground)
exec /usr/sbin/apache2ctl -DFOREGROUND
