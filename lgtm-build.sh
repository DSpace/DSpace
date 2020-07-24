$! /bin/sh

mvn clean package \
	-f  pom.xml \
	-B -V -e \
	-Dfindbugs.skip \
	-Dcheckstyle.skip \
	-Dpmd.skip=true \
	-Denforcer.skip \
	-Dmaven.javadoc.skip \
	-DskipTests=true \
	-Dmaven.test.skip=false \
	-Dlicense.skip=true \
	-Drat.skip=true \
	-Dmaven.repo.local=/opt/work/semmle_data/maven_repo \
	-t /opt/work/.m2/toolchains.xml
