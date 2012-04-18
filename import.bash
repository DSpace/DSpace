#!/bin/bash
set -e

OLD_CWD=`pwd`
BASE_DIR=$(cd `dirname $0` && pwd)
source $BASE_DIR/build.defaults


echo -e "Creamos un administrador responsable de la importacion "
$INSTALL_DIR/bin/dspace create-administrator --email export@sedici.unlp.edu.ar --first Export --last Export --language es --password "export_$RANDOM"

* Ejecutar todos los pasos del proceso de importacion descripto en http://trac.prebi.unlp.edu.ar/projects/sedici-dspace/wiki/Pasos_de_importaci%C3%B3n (tener paciencia)
* Ejecutar la indexacion de los datos en el postgreSQL (/install/bin/dspace update-index)
* Ejecutar la indexacion de los datos en el Solr (/install/bin/dspace update-discovery-index)
