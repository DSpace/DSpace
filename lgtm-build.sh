$! /bin/sh

/opt/dist/tools/preload_tracer /opt/out/snapshot/workspace/apache-maven-3.6.0/bin/mvn clean package \
	-f  pom.xml \
	-B -V -e \
	-Dfindbugs.skip \
	-Dcheckstyle.skip \
	-Dpmd.skip=true \
	-Denforcer.skip \
	-Dmaven.javadoc.skip \
	-DskipTests \
	-Dlicense.skip=true \
	-Drat.skip=true \
	-Dmaven.repo.local=/opt/work/semmle_data/maven_repo \
	-t /opt/work/.m2/toolchains.xml
