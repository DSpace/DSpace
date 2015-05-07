#!/bin/bash
set -e


#JAVA_OPTS="JAVA_OPTS=-Xmx1024m"

#ANT_OPTS="-q"
#MVN_OPTS="-q"
MVN_PROFILES=\!dspace-jspui,\!dspace-rdf,\!dspace-sword,\!dspace-swordv2
#,\!dspace-rest
# -Dhttp.proxyHost=10.1.0.27 -Dhttp.proxyPort=3128 -Dhttps.proxyHost=10.1.0.27 -Dhttps.proxyPort=3128"

cwd=`pwd`
DSPACE_SRC=$(dirname $(readlink -f $0))
DSPACE_DIR=$DSPACE_SRC/install
DSPACE_USER=`whoami`
TOMCAT="tomcat7"

#/var/dspace/source
#/var/dspace/install


install() { 

        if [ -d "$DSPACE_DIR" ] ; then
                read -n 1 -p "El directorio de instalacion ( $DSPACE_DIR ) ya existe, desea eliminarlo [YySs]? "
                if [[ ! $REPLY =~ ^[YySs]$ ]]; then
                    exit 1
                fi
                rm -fr $DSPACE_DIR
        fi
        mkdir $DSPACE_DIR

	#si ya existe una instalacion vieja
	#if db exists

	#test: 
	psql -h data.sedici.unlp.edu.ar -Udspace_cic -W
	dropdb --username=pg_root --host=data.sedici.unlp.edu.ar dspace_cic --if-exists -i
#endif
createdb --username=pg_root --encoding=UNICODE --owner=dspace_cic --host=data.sedici.unlp.edu.ar dspace_cic

#En caso que sea una versión snapshot, se compila en el nivel base de dspace-src para que maven encuentre las dependencias
cd $DSPACE_SRC
mvn install clean $MVN_OPTS -P $MVN_PROFILES

cd $DSPACE_SRC/dspace 
mvn package $MVN_OPTS -P $MVN_PROFILES

sudo /etc/init.d/$TOMCAT stop

cd $DSPACE_SRC/dspace/target/dspace-installer/
ant fresh_install $ANT_OPTS

#importación de metadatos CIC
$DSPACE_DIR/bin/dspace dsrun org.dspace.administer.MetadataImporter -f $DSPACE_DIR/config/registries/cic-types.xml


$DSPACE_DIR/bin/dspace create-administrator

#if --deploy
rm -r /var/lib/$TOMCAT/webapps/ROOT
ln -s $DSPACE_DIR/webapps/xmlui/ /var/lib/$TOMCAT/webapps/ROOT
ln -s $DSPACE_DIR/webapps/solr/ /var/lib/$TOMCAT/webapps/solr
#endif


sudo /etc/init.d/$TOMCAT start

#Inicialización de los índices de discovery
./dspace index-discovery


}

update()
{
	show_message "Actualizando la instalacion de DSpace. Esta operación suele demorar un par de minutos."
	
	show_message "Actualizamos el código fuente de github"
	cd $DSPACE_SRC
	git stash && git pull --rebase && git stash pop
	
	show_message "Empaquetamos dspace"
	cd dspace 
	$JAVA_OPTS mvn package $MVN_OPTS -P $MVN_PROFILES
	
	show_message "Paramos el tomcat"
	sudo /etc/init.d/$TOMCAT stop
	
	#Limpiar cache XMLUI/Cocoon
	#sudo rm /var/lib/$TOMCAT/work/Catalina/localhost/_/cache-dir/cocoon-ehcache.data
	#sudo rm /var/lib/$TOMCAT/work/Catalina/localhost/_/cache-dir/cocoon-ehcache.index

	show_message "actualizamos los sources"
	cd target/dspace-installer
	#TODO reusar el /var/dspace/install/config/GeoLiteCity.dat
	$JAVA_OPTS ant update $ANT_OPTS 
	
	show_message "eliminamos directorios de bkp viejos"
	ant clean_backups -Ddspace.dir=$DSPACE_SRC
	#rm -r $DSPACE_DIR/bin.bak-* $DSPACE_DIR/etc.bak-* $DSPACE_DIR/lib.bak-* $DSPACE_DIR/webapps.bak-*
	cd $DSPACE_SRC
	$JAVA_OPTS mvn clean
	
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


if [ "`whoami`" != "$DSPACE_USER" ]; then
    show_message "You must run this script using dspace user $DSPACE_USER"
    exit 1
fi

case "$1" in
  install)
	install
        ;;
  update)
	update
        ;;
  *)
        echo "Usage: $0 {install|update}" >&2
        exit 3
        ;;
esac

exit 0

