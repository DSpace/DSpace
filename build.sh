#!/bin/bash
set -e


JAVA_OPTS="-Xmx1024m"
MVN_OPTS="-q -P \!dspace-jspui,\!dspace-rdf,\!dspace-sword,\!dspace-swordv2"
# -Dhttp.proxyHost=10.1.0.27 -Dhttp.proxyPort=3128 -Dhttps.proxyHost=10.1.0.27 -Dhttps.proxyPort=3128"

cwd=`pwd`
DSPACE_SRC=$(dirname $(readlink -f $0))
DSPACE_DIR=$DSPACE_SRC/install
TOMCAT="tomcat7"

#/var/dspace/source
#/var/dspace/install

install()
{
mvn package  -P $MVN_PROFILES

}

update()
{
	show_message "Actualizando la instalacion de DSpace. Esta operación suele demorar un par de minutos."
	
	show_message "Actualizamos el código fuente de github"
	cd $DSPACE_SRC
	git stash && git pull && git rebase && git stash pop
	
	show_message "Empaquetamos dspace"
	cd dspace 
	$JAVA_OPTS mvn package $MVN_OPTS
	
	show_message "Paramos el tomcat"
	sudo /etc/init.d/$TOMCAT stop
	
	#Limpiar cache XMLUI/Cocoon
	#sudo rm /var/lib/$TOMCAT/work/Catalina/localhost/_/cache-dir/cocoon-ehcache.data
	#sudo rm /var/lib/$TOMCAT/work/Catalina/localhost/_/cache-dir/cocoon-ehcache.index
	
	show_message "actualizamos los sources"
	cd target/dspace-installer
	ANT_OPTS=$JAVA_OPTS ant update -q 
	
	show_message "eliminamos directorios de bkp viejos"
	ant clean_backups -Ddspace.dir=$DSPACE_DIR
	#rm -r $DSPACE_DIR/bin.bak-* $DSPACE_DIR/etc.bak-* $DSPACE_DIR/lib.bak-* $DSPACE_DIR/webapps.bak-*
	cd $DSPACE_SRC
	$JAVA_OPTS mvn clean $MAVEN_OPTS
	
	show_message "iniciamos tomcat"
	sudo /etc/init.d/$TOMCAT start
	
	show_message "Se hicieron los siguientes reemplazos en la configuración"
	find  $DSPACE_DIR/config/ -name "*.old"
	
	show_message "Se actualizó correctamente dspace"
	cd $cwd

}

show_message()
{
   echo "#################################################################################################"
   echo "#################################################################################################"
   echo "# $1 #"
   echo "#################################################################################################"
   echo "#################################################################################################"
}

