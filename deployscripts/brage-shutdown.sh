#!/bin/sh
#  ____ _____ ____   _______     _______   ____  _____            _____ ______
# |  _ \_   _|  _ \ / ____\ \   / / ____| |  _ \|  __ \     /\   / ____|  ____|
# | |_) || | | |_) | (___  \ \_/ / (___   | |_) | |__) |   /  \ | |  __| |__
# |  _ < | | |  _ < \___ \  \   / \___ \  |  _ <|  _  /   / /\ \| | |_ |  __|
# | |_) || |_| |_) |____) |  | |  ____) | | |_) | | \ \  / ____ \ |__| | |____
# |____/_____|____/|_____/   |_| |_____/  |____/|_|  \_\/_/    \_\_____|______|
#
# Bibsys Brage runs in two tomcat instances (XMLUI+OAI, SOLR). We are using a single tomcat binary
# distribution executed as two instances. In order to make this possible we are
# exporting CATALINA_BASE (instance config dir) before executing the shutdown script
# (function stop_base)
#
stop_base () {
  echo
  echo "Shutting down ${1}..."
  export CATALINA_BASE=$2
  $CATALINA_HOME/bin/shutdown.sh
  if [ $? -eq 0 ]; then
      echo "${1} shutdown successful"
  else
      echo "${1} shutdown failed"
  fi
  return $?
}
########### SANITY CHECKS ####################
if [ ! ${JAVA_HOME} ]; then
    echo "JAVA_HOME not set!"
    exit 1
elif [ ! ${CATALINA_HOME} ]; then
    echo "CATALINA_HOME not set!"
    exit 1
elif [ ! ${CATALINA_BASE_XMLUI_OAI} ]; then
    echo "CATALINA_BASE_XMLUI_OAI not set!"
    exit 1
elif [ ! ${CATALINA_BASE_SOLR} ]; then
    echo "CATALINA_BASE_SOLR not set!"
    exit 1
fi
########## /SANITY CHECKS ###################

echo "Shutting down Tomcat..."

stop_base "Tomcat-XMLUI/OAI" $CATALINA_BASE_XMLUI_OAI
XMLUI_OAI_SHUTDOWN_STATUS=$?

stop_base "Tomcat-SOLR" $CATALINA_BASE_SOLR
SOLR_SHUTDOWN_STATUS=$?

echo
if [ $XMLUI_OAI_SHUTDOWN_STATUS -a $SOLR_SHUTDOWN_STATUS ]; then
    echo "Tomcat shutdown successful"
    exit 0
else
    echo "Tomcat shutdown failed"
    exit 1
fi
