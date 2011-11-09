#!/bin/bash
set -e
seed="$RANDOM"
DSPACE_VERSION="1.8.0-rc1"

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
    print_sec "EMPAQUETADO DEL PROYECTO"
    
	MVN_ARGS=" -Ddefault.dspace.dir=$DATA_DIR "
	mvn clean license:format install $MVN_ARGS

	echo -e "\n==========ACTUALIZANDO DSPACE@SEDICI"
	cd $BASE_DIR/distribution/target/dspace-sedici-distribution-bin
	ant  -Ddspace.dir=$DATA_DIR -Ddspace.configuration=$DATA_DIR/config/dspace.cfg -Doverwrite=false update
	
    print_sec "Felicitaciones! se actualizo correctamente Dspace@SeDiCI en \n\t $DATA_DIR"
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
	mvn clean license:format install $MVN_ARGS

	print_sec "CREACION DE LA BBDD"
	echo -e "A continuacion ingrese '$dspace_dbpassword' 2 veces"
	sudo su -c "createuser -d -P -R -S $dspace_dbuser" postgres

	echo -e "\nA continuacion ingrese '$dspace_dbpassword' "
	sudo su -c "createdb -U $dspace_dbuser -E UNICODE $dspace_dbname -h localhost" postgres
	#dropdb dspace
	#createdb -U $dspace_user -E UNICODE $dspace_dbname -h localhost

	print_sec "INSTALANDO DSPACE@SEDICI"
	cd $BASE_DIR/distribution/target/dspace-sedici-distribution-binorg.postgresql.util.PSQLException: FATAL: password authentication failed for user "dspace_user"
	ant -Ddspace.dir=$DATA_DIR fresh_install -Dgeolite=$BASE_DIR/config/GeoLiteCity.dat.gz

	$DATA_DIR/bin/dspace create-administrator
	#sudo chown -R myUser.myGroup $DATA_DIR
	chmod -R a+rw $DATA_DIR/log

    print_sec "Felicitaciones! se instalo correctamente Dspace@SeDiCI en \n\t $DATA_DIR \n\t con la BBDD $dspace_dbname"
}

#=============================================================================

if [ $1 -a $1 == "update" ]; then
        do_update
else
        do_install
fi

echo "Limpiando un poco. Se empaqueta de vuelta xmlui , para que quede xmlui/target filtrado..."
cd $BASE_DIR/xmlui
mvn clean package -Ddspace.dir=$DATA_DIR -q

cd $OLD_CWD

print_sec "A-leluia, a-lelu, a-leluia, a-leuia, a-leluuu-ia! "

exit 1;

