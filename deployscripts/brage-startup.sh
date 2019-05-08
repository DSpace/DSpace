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
# exporting CATALINA_BASE (instance config dir) before executing the startup script
# (function start_base)
#
start_base () {
    echo
    echo "Starting ${1}..."
    export CATALINA_BASE=$2
    $CATALINA_HOME/bin/startup.sh

    if [ $? -ne 0 ]; then
	echo "${1} failed to start"
	exit 1
    fi
    
    echo "${1} started successfully"
}
########### SANITY CHECKS ####################
if [ ! $JAVA_HOME ]; then
    echo "JAVA_HOME not set!"
    exit 1
elif [ ! $CATALINA_HOME ]; then
    echo "CATALINA_HOME not set!"
    exit 1
elif [ ! $CATALINA_BASE_XMLUI_OAI ]; then
    echo "CATALINA_BASE_XMLUI_OAI not set!"
    exit 1
elif [ ! $CATALINA_BASE_SOLR ]; then
    echo "CATALINA_BASE_SOLR not set!"
    exit 1
fi
########## /SANITY CHECKS ###################

######### ADDITIONAL ENVIRONMENT VARIABLES ################
if [ $LD_LIBRARY_PATH ]; then
    case :$LD_LIBRARY_PATH: in
	*:$CATALINA_HOME/lib:*)  ;;  # do nothing
	*) LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$CATALINA_HOME/lib 
	   export LD_LIBRARY_PATH
	   ;;
    esac
else
    export LD_LIBRARY_PATH=$CATALINA_HOME/lib
fi
########## /ADDITIONAL ENVIRONMENT VARIABLES ###############

#Default file/folder permissions
umask 003

#Max number of open files
ulimit -n 30000

echo "Starting Tomcat..."
start_base "Tomcat-SOLR" $CATALINA_BASE_SOLR
start_base "Tomcat-XMLUI/OAI" $CATALINA_BASE_XMLUI_OAI
echo
echo "Tomcat started successfully"
exit 0






