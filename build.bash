#!/bin/bash
set -e
seed="$RANDOM"
DSPACE_VERSION="1.8.2"

dspace_dbuser="dspace_user$seed"
dspace_dbname="dspace$seed"
dspace_dbpassword="dspace"

OLD_CWD=`pwd`

BASE_DIR=$(cd `dirname $0` && pwd)
DATA_DIR=$BASE_DIR/install

#=============================================================================

print_sec(){
    echo -e "\n===================================================="
	echo -e "==========$1=========="
    echo "===================================================="
}
do_update(){
	print_sec 'COMIENZA LA ACTUALIZACION'
	
	if [ ! -d "$DATA_DIR" ] ; then
		echo "El diretorio de instalacion no existe ($DATA_DIR). Seguro que queres hacer un update? " 
		exit 0;
	fi
    
	MVN_ARGS=" -Ddefault.dspace.dir=$DATA_DIR "
		
	
	if [ $1  ]; then
		if [ "$1" == "full"  ]; then
			print_sec "EMPAQUETADO DEL PROYECTO FULLLLLLLLL!!!";
	  	else
		  echo "Elegí: update full o update, pero no $1";
		fi
	else
		MVN_ARGS="$MVN_ARGS -Dsedici.min_package=true"
	  	print_sec "EMPAQUETADO DEL PROYECTO parcial";
	fi
	mvn clean license:format install $MVN_ARGS

	echo -e "\n==========ACTUALIZANDO DSPACE@SEDICI"
	cd $BASE_DIR/distribution/target/dspace-sedici-distribution-bin
	ant  -Ddspace.dir=$DATA_DIR -Ddspace.configuration=$DATA_DIR/config/dspace.cfg -Doverwrite=false update
	
    print_sec "Felicitaciones! se actualizo correctamente Dspace@SeDiCI en \n\t $DATA_DIR"
}
install_vhost()
{
	print_sec "habilito el vitual host"
	sudo a2enmod proxy proxy_ajp
	sudo cp $DATA_DIR/config/httpd/dspace.vhost /etc/apache2/sites-available/dspace
	sudo a2ensite dspace
	sudo service apache2 restart
	
	if (! resolveip -s "dspace.localhost") ; then
        echo '127.0.0.1 dspace.localhost' | sudo tee --append /etc/hosts
	fi
	
	if [ -d "$CATALINA_HOME" ] ; then
		echo "Se crean los links simbólicos a las webapps en $CATALINA_HOME/webapps"
		
		#check 	<Connector port="8009" protocol="AJP/1.3" redirectPort="8443"/>
		echo "Recuerde chequear el archivo $CATALINA_HOME/conf/server.xml debe tener el conector para AJP/1.3 habilitado en el puerto 8009" 
		
		sudo mv $CATALINA_HOME/webapps/ROOT $CATALINA_HOME/webapps/ROOT_hold
		sudo ln -s $DATA_DIR/webapps/xmlui $CATALINA_HOME/webapps/ROOT 
		sudo ln -s $DATA_DIR/webapps/oai $CATALINA_HOME/webapps/oai
		sudo ln -s $DATA_DIR/webapps/solr $CATALINA_HOME/webapps/solr
	else
		echo "No se crean los links simbólicos a las webapps porque no se encontro la variable de entorno CATALINA_HOME"
	fi
	 
}

do_install()
{
	print_sec "COMIENZA LA INSTALACION"
	
	if [ -d "$DATA_DIR" ] ; then
		read -n 1 -p "El directorio de instalacion ( $DATA_DIR ) ya existe, desea eliminarlo [YySs]? "
		if [[ ! $REPLY =~ ^[YySs]$ ]]; then
		    exit 1
		fi
		rm -fr $DATA_DIR
	fi
	mkdir $DATA_DIR

	print_sec "EMPAQUETADO DEL PROYECTO"
	MVN_ARGS=" -Ddefault.dspace.dir=$DATA_DIR -Ddefault.db.username=$dspace_dbuser -Ddefault.db.password=$dspace_dbpassword -Ddefault.db.url=jdbc:postgresql://localhost:5432/$dspace_dbname"
	mvn clean license:format install $MVN_ARGS $EXTRA_ARGS

	print_sec "CREACION DE LA BBDD"
	echo -e "A continuacion ingrese '$dspace_dbpassword' 2 veces"
	sudo su -c "createuser -d -P -R -S $dspace_dbuser" postgres

	echo -e "\nA continuacion ingrese '$dspace_dbpassword' "
	sudo su -c "createdb -U $dspace_dbuser -E UNICODE $dspace_dbname -h localhost" postgres
	#dropdb dspace
	#createdb -U $dspace_user -E UNICODE $dspace_dbname -h localhost

	print_sec "INSTALANDO DSPACE@SEDICI"
	cd $BASE_DIR/distribution/target/dspace-sedici-distribution-bin
	ant -Ddspace.dir=$DATA_DIR fresh_install -Dgeolite=$BASE_DIR/config/GeoLiteCity.dat.gz

	echo -e "Personalizando los metadatos"
	$DATA_DIR/bin/dspace dsrun org.dspace.administer.MetadataImporter -f $DATA_DIR/config/registries/sedici-metadata.xml
	$DATA_DIR/bin/dspace dsrun org.dspace.administer.MetadataImporter -f $DATA_DIR/config/registries/sedici2003-metadata.xml

	echo -e "Instalando XMLWorkflow"
	WORKFLOW_SCRIPTS="$DATA_DIR/etc/postgres/xmlworkflow"
	$DATA_DIR/bin/dspace dsrun org.dspace.storage.rdbms.InitializeDatabase $WORKFLOW_SCRIPTS/xml_workflow.sql
	$DATA_DIR/bin/dspace dsrun org.dspace.storage.rdbms.InitializeDatabase $WORKFLOW_SCRIPTS/workflow_migration.sql
	$DATA_DIR/bin/dspace dsrun org.dspace.storage.rdbms.InitializeDatabase $WORKFLOW_SCRIPTS/initialize.sql

	$DATA_DIR/bin/dspace create-administrator
	#sudo chown -R myUser.myGroup $DATA_DIR
	chmod -R a+rw $DATA_DIR/log $DATA_DIR/assetstore $DATA_DIR/upload
	
	echo -e "Instalando el virtual host de apache con mod_proxy"
	install_vhost 
	
    print_sec "Felicitaciones! se instalo correctamente Dspace@SeDiCI en \n\t $DATA_DIR \n\t con la BBDD $dspace_dbname"
}

#=============================================================================

if [ $1 -a $1 == "update" ]; then
        do_update $2
elif [ $1 -a $1 == "install" ]; then
        do_install $2
else
	echo "Elegí: (update | install) full?"
	exit 0;
fi



echo "Limpiando un poco. Se empaqueta de vuelta xmlui , para que quede xmlui/target filtrado..."
cd $BASE_DIR/xmlui
mvn clean package -Ddspace.dir=$DATA_DIR -q

cd $OLD_CWD

print_sec "A-leluia, a-lelu, a-leluia, a-leuia, a-leluuu-ia! "

exit 1;

