#!/bin/bash

#http://stackoverflow.com/questions/368744/shell-scripting-die-on-any-error
#set -e

OLD_CWD=`pwd`
BASE_DIR=$(cd `dirname $0` && pwd)
source $BASE_DIR/build.defaults
current_user=`whoami`

MVN_ARGS=" -Ddefault.dspace.dir=$INSTALL_DIR "

#MVN_ARGS="$MVN_ARGS -Pxpdf-mediafilter-support "

#=============================================================================

print_help()
{
        echo "Usage: `basename $0` (install|update) [OPTION]"
        echo "donde OPTION puede ser uno de:"
        echo -e " --full\t\tRecompila las dependencias con SeDiCI2003 (aplicable solo para update)"
        echo -e " --deploy\t\t Hace un deploy completo (aplicable solo para install)"
        echo ""
        exit 0;
}


print_sec()
{
    echo -e "\n===================================================="
	echo -e "==========$1=========="
    echo "===================================================="
}

print_err(){
    echo -e "\n===================================================="
	echo -e "==========$1=========="
    echo "===================================================="
    exit 0;
}

#Crea el usuario y grupo para dspace y para el web container.
create_user()
{

        sudo adduser -q --group $dspace_group 2>/dev/null

#	if [ ! "`sudo groupmod $dspace_group 2>/dev/null`" ]; then
#	    sudo groupadd $dspace_group
        echo "Se creo el dspace_group $dspace_group, no se si existia, pero se trato de crear"
#	else
#		echo "Ya existe el dspace_group $dspace_group"
#	fi

	if [ ! "`id $dspace_user 2>/dev/null`" ]; then
	    sudo useradd -d $BASE_DIR -g $dspace_group $dspace_user
        sudo chfn -o "umask=002" $dspace_user
       echo "Se creo el dspace_user $dspace_user"
else
		echo "Ya existe el dspace_user $dspace_user"
	fi
	
  	if [ "x$web_user" != "x$dspace_user" ]; then
 	
	 	  sudo adduser -q --group $web_group 2>/dev/null
#		if [ "`sudo groupmod $web_group`" ]; then
#		    sudo groupadd $web_group
	        echo "Se creo el web_group $web_group"
#		else
#	        echo "El web_group $web_group ya existe"
#		fi
	
	 	if [ ! "`id $web_user 2>/dev/null`" ]; then
		    sudo useradd -d $BASE_DIR -g $web_group $web_user
		    sudo chfn -o "umask=002" $web_user
		    echo "Se creo el web_user $web_user"
		else
	 		tomcatumask=`sudo su -c 'umask' $web_user`
	 		if [ "x$tomcatumask" != "x002" -a "x$tomcatumask" != "x0002" ]; then
	 			sudo chfn -o "umask=002" $web_user
	 			echo "Como el umask del usuario $web_user era $tomcatumask , se sobreescribió con 002" 
	        else
	        	echo "El umask del usuario $web_user ya es 002" 
	        fi
        fi
        
 	if [ -z "`id $web_user 2>/dev/null  | grep $dspace_group`" ]; then
	 		 sudo adduser $web_user $dspace_group
 			 echo "Se agrego el web_user $web_user al dspace_group $dspace_group" 
        else
       	 echo "El web_user $web_user ya pertenece al dspace_group $dspace_group" 
        fi
    else
  	echo "el web_user es igual al dspace_user, $web_user"
fi
}

install_httpd_vhost()
{
	print_sec "habilito el vitual host"
	sudo a2enmod proxy proxy_ajp
	sudo cp $INSTALL_DIR/config/httpd/dspace.vhost /etc/apache2/sites-available/dspace
	sudo a2ensite dspace
	sudo service apache2 restart
	
	if (! resolveip -s "dspace.localhost") ; then
        echo '127.0.0.1 dspace.localhost' | sudo tee --append /etc/hosts
	fi
}

install_webapps()
{
	if [ ! "$web_home" ] ; then
		web_home=$CATALINA_HOME/webapps
	fi
	
	if [ -d "$web_home" ] ; then
		echo "Se crean los links simbólicos a las webapps en $web_home"
		
		#check 	<Connector port="8009" protocol="AJP/1.3" redirectPort="8443"/>
		echo "Recuerde chequear el archivo $web_home/../conf/server.xml debe tener el conector para AJP/1.3 habilitado en el puerto 8009" 
		
		sudo mv $web_home/ROOT $web_home/ROOT_old
		sudo ln -s $INSTALL_DIR/webapps/xmlui $web_home/ROOT 
		sudo ln -s $INSTALL_DIR/webapps/oai $web_home/oai
		sudo ln -s $INSTALL_DIR/webapps/xoai $web_home/xoai
		sudo ln -s $INSTALL_DIR/webapps/solr $web_home/solr
	else
		echo "No se crean los links simbólicos a las webapps porque no se encontro la variable de entorno CATALINA_HOME ni web_home"
	fi
}

do_install()
{
	print_sec "COMIENZA LA INSTALACION"
	
	if [ -d "$INSTALL_DIR" ] ; then
		read -n 1 -p "El directorio de instalacion ( $INSTALL_DIR ) ya existe, desea eliminarlo [YySs]? "
		if [[ ! $REPLY =~ ^[YySs]$ ]]; then
		    exit 1
		fi
		rm -fr $INSTALL_DIR
	fi
	mkdir $INSTALL_DIR

	print_sec "EMPAQUETADO DEL PROYECTO"
	MVN_ARGS="$MVN_ARGS -Ddefault.db.username=$dspace_dbuser -Ddefault.db.password=$dspace_dbpassword -Ddefault.db.url=jdbc:postgresql://$pg_connection_host:5432/$dspace_dbname"
	mvn clean license:format install $MVN_ARGS $EXTRA_ARGS

	print_sec "CREACION DE LA BBDD"
	# si la BD esta en localhost usamos el usuario "postgres" usando sudo (para evitar tener que crear un superusuario en una BD local).
	# de lo contrario, usamos el createuser de forma remota (para lo cual se necesita un superusuario con password)
	echo -e "Creando el usuario $dspace_dbuser en el PostgreSQL"
	if [ $pg_connection_host = "localhost" ]; then
		echo -e "A continuacion ingrese la password para el usuario $dspace_dbuser ('$dspace_dbpassword') 2 veces"
		sudo su -c "createuser -d -P -R -S $dspace_dbuser" postgres
	else
		echo -e "Ingrese primero la password para el nuevo usuario $dspace_dbuser ('$dspace_dbpassword') 2 veces y luego la password del superusuario '$pg_connection_user' para conectar con el servidor"
		createuser -U $pg_connection_user -h $pg_connection_host -d -P -R -S $dspace_dbuser
	fi
	
	echo -e "\nA continuacion ingrese '$dspace_dbpassword' para conectar como $dspace_dbuser"
	#sudo su -c "createdb -U $dspace_dbuser -E UNICODE $dspace_dbname -h localhost" postgres
	createdb -h $pg_connection_host -U $dspace_dbuser -E UNICODE $dspace_dbname

	print_sec "INSTALANDO DSPACE@SEDICI"
	cd $BASE_DIR/dspace/target/dspace-sedici-distribution-bin
	ant -Ddspace.dir=$INSTALL_DIR fresh_install -Dgeolite=$BASE_DIR/config/GeoLiteCity.dat.gz

	echo -e "Personalizando los metadatos"
	$INSTALL_DIR/bin/dspace dsrun org.dspace.administer.MetadataImporter -f $INSTALL_DIR/config/registries/sedici-metadata.xml
	$INSTALL_DIR/bin/dspace dsrun org.dspace.administer.MetadataImporter -f $INSTALL_DIR/config/registries/sedici2003-metadata.xml

	echo -e "Instalando XMLWorkflow"
	WORKFLOW_SCRIPTS="$INSTALL_DIR/etc/postgres/xmlworkflow"
	$INSTALL_DIR/bin/dspace dsrun org.dspace.storage.rdbms.InitializeDatabase $WORKFLOW_SCRIPTS/xml_workflow.sql
	$INSTALL_DIR/bin/dspace dsrun org.dspace.storage.rdbms.InitializeDatabase $WORKFLOW_SCRIPTS/workflow_migration.sql

	echo -e "Creamos un administrador (mas adelante podra crear mas desde $INSTALL_DIR/bin/dspace create-administrator)"
	$INSTALL_DIR/bin/dspace create-administrator
	
	echo -e "Se verifican los users y groups"
	create_user

	echo -e "Se setean los permisos para los directorios de trabajo log, assetstore, etc"
	chmod -R ug+rw,o-w $INSTALL_DIR/log $INSTALL_DIR/assetstore $INSTALL_DIR/upload $INSTALL_DIR/solr/search $INSTALL_DIR/solr/statistics  $INSTALL_DIR/exports  $INSTALL_DIR/reports 
	sudo chown -R $dspace_user.$dspace_group $INSTALL_DIR
	
	if [ "x$1" = "x--deploy" ]; then
		print_sec "Instalando el virtual host de apache con mod_proxy"
		install_httpd_vhost
		 
		print_sec "Se hace el deploy dentro de tomcat"
		install_webapps
	fi
	
    print_sec "Felicitaciones! se instalo correctamente Dspace@SeDiCI en \n\t $INSTALL_DIR \n\t con la BBDD $dspace_dbname"
}

do_update(){
	print_sec 'COMIENZA LA ACTUALIZACION'
	
	if [ ! -d "$INSTALL_DIR" ] ; then
		print_err "El diretorio de instalacion no existe ($INSTALL_DIR)"
	fi
    
	if [ "x$1" = "x--full"  ]; then
		print_sec "EMPAQUETADO DEL PROYECTO FULL!!!";
	else
		MVN_ARGS="$MVN_ARGS -Dsedici.min_package=true"
	  	print_sec "EMPAQUETADO DEL PROYECTO parcial";
	fi
	mvn clean license:format install $MVN_ARGS

	echo -e "\n==========ACTUALIZANDO DSPACE@SEDICI"
	cd $BASE_DIR/dspace/target/dspace-sedici-distribution-bin
	 
	if [ ! "`id $current_user 2>/dev/null  | grep $dspace_group`" ]; then
		print_err "El usuario que está ejecutando el script ($current_user) no es miembro del dspace_group $dspace_group. Por lo tanto no podrá modificar el directorio $INSTALL_DIR"
		#No se puede ejecutar el siguiente comando como dspace porque ant crea directorios temporales en ../
		#sudo su -c "$ANT_UPDATE" $dspace_user
	fi
	
	ant -Ddspace.dir=$INSTALL_DIR -Ddspace.configuration=$INSTALL_DIR/config/dspace.cfg -Doverwrite=false update

	echo -e "\n=========A revisar los siguientes archivos de configuracion"
	diff -r $INSTALL_DIR/config $BASE_DIR/distribution/overwritten-files/config -y --suppress-common-lines --width=180 | grep -v Only | grep -v '\${.*}'

        print_sec "Felicitaciones! se actualizo correctamente Dspace@SeDiCI en \n\t $INSTALL_DIR"
}

#=============================================================================

echo "`whoami` dame tus privilegios de root, dame tu poder!"

case "$1" in
    install )
		sudo ls > /dev/null 
		do_install $2
    	;;
    update )
		do_update $2
    	;;
    * )
		print_help
    	;;        
esac

echo "Limpiando un poco"
mvn clean


cd $OLD_CWD 

print_sec "A-leluia, a-lelu, a-leluia, a-leuia, a-leluuu-ia! "

exit 1;

