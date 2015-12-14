#!/bin/bash
#
# LINDAT/CLARIN setup libs
#

OUTDIR="../bits/libs"
if [ ! -d "${OUTDIR}" ]; then
    mkdir ${OUTDIR}
fi


download() {
    VER=$1
    if [ ! -f "${OUTDIR}/${VER}" ]; then
        echo "Downloading ${VER} library"
        wget -q http://www.java2s.com/Code/JarDownload/jai/${VER}.zip
        unzip -q  ${VER}.zip
        mv ${VER} ${OUTDIR}
        rm -f ${VER}.zip
    fi
}


VER_JAI_CORE=jai_core-1.1.2_01.jar
VER_JAI_IMAGE=jai_imageio-1.0_01.jar

download ${VER_JAI_CORE}
download ${VER_JAI_IMAGE}


echo "Installing libraries"

# leave the version cause of dspace pom
mvn install:install-file -Dfile=${OUTDIR}/${VER_JAI_IMAGE} -DgroupId=com.sun.media -DartifactId=jai_imageio -Dversion=1.0_01 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=${OUTDIR}/${VER_JAI_CORE} -DgroupId=javax.media -DartifactId=jai_core -Dversion=1.1.2_01 -Dpackaging=jar -DgeneratePom=true

COG_JGLOBUS=cog-jglobus-1.8.0.jar
if [ -f "${OUTDIR}/${COG_JGLOBUS}" ]; then
    mvn install:install-file -Dfile=${OUTDIR}/${COG_JGLOBUS} -DgroupId=org.globus.jglobus -DartifactId=cog-jglobus -Dversion=1.8.0 -Dpackaging=jar -DgeneratePom=true
fi

PURETLS=puretls-1.1.jar
if [ -f "${OUTDIR}/${PURETLS}" ]; then
    mvn install:install-file -Dfile=${OUTDIR}/${PURETLS} -DgroupId=com.claymoresystems -DartifactId=puretls -Dversion=1.1 -Dpackaging=jar -DgeneratePom=true
fi

#VER_JARGON=jargon.jar
#if [ -f "${OUTDIR}/${VER_JARGON}" ]; then
#    mvn install:install-file -Dfile=${OUTDIR}/${VER_JARGON} -DgroupId=edu.sdsc.grid -DartifactId=jargon -Dversion=3.0.0 -Dpackaging=jar -DgeneratePom=true
#else
#    echo "\nWARNING: You must download ${VER_JARGON} version 3.0.0 and install it!"
#fi



