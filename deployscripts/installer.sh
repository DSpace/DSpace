#!/bin/sh
#  ____ _____ ____   _______     _______   ____  _____            _____ ______
# |  _ \_   _|  _ \ / ____\ \   / / ____| |  _ \|  __ \     /\   / ____|  ____|
# | |_) || | | |_) | (___  \ \_/ / (___   | |_) | |__) |   /  \ | |  __| |__
# |  _ < | | |  _ < \___ \  \   / \___ \  |  _ <|  _  /   / /\ \| | |_ |  __|
# | |_) || |_| |_) |____) |  | |  ____) | | |_) | | \ \  / ____ \ |__| | |____
# |____/_____|____/|_____/   |_| |_____/  |____/|_|  \_\/_/    \_\_____|______|
#
# Automated installer script for jenkins
#
# Prerequisites: JAVA_HOME, CATALINA_HOME, CATALINA_BASE_XMLUI_OAI, CATALINA_BASE_SOLR, ANT_HOME already set in environment
# Steps:
#           1. Extract ant installer to $SCRIPT_DIR/$EXTRACT_DIR
#           2. Iff latest backup: Roll over $SCRIPT_DIR/backup/latest to $SCRIPT_DIR/backup/brage_<YYYYmmDD>T<HHMMSS>.tar.gz
#           3. Iff existing brage installation: $DSPACE_DIR to $SCRIPT_DIR/backup/latest/brage_app.tar.gz
#           4. Iff existing brage database: Backup $DATABASE to $SCRIPT_DIR/backup/latest/$DATABASE.sql
#           5. Run ant install ($SCRIPT_DIR/$EXTRACT_DIR)
#           6. Install BIBSYS scripts ($DSPACE_DIR/${BIBSYS_SCRIPT_DIR})
#           7. Shutdown tomcat - prepare for new version
#           8. Remove existing webapps from [$CATALINA_BASE_XMLUI_OAI/$CATALINA_BASE_SOLR]/webapps (solr, oai, xmlui)
#           9. Iff $DATABASE has pending migrations - run $DATABASE migrations
#           10. Copy $DSPACE_DIR/webapps to [$CATALINA_BASE_XMLUI_OAI/$CATALINA_BASE_SOLR]/webapps (solr, oai, xmlui)
#           11. Create missing top communities (registered in institution-registry.cfg, but missing in database)
#           12. Dynamically map cristin workflow (XML workflow) to cristin collections
#           13. Dynamically map community themes to handles
#           14. Start tomcat - run new version of Brage
#           15. Iff migrations - update app index and oai index
#           16. Install crontab for user (crontab_template)

SCRIPT_DIR=`dirname $0`
SCRIPT_NAME=`basename $0`
BIBSYS_SCRIPT_DIR=deployscripts

# Make sure base path is where we currently are
cd ${SCRIPT_DIR}
# backup folders
BACKUP_DIR=${SCRIPT_DIR}/backup
LATEST_BACKUP_DIR=${BACKUP_DIR}/latest
# /backup folders
INSTALLER=`find . * -maxdepth 0 -name $(echo ${SCRIPT_NAME} | awk -F. '{print $1}').tar.gz -printf "%f"`

########### SANITY CHECKS ####################
if [ ! ${JAVA_HOME} ]; then
    echo "JAVA_HOME not set!"
    exit 1
elif [ ! ${ANT_HOME} ]; then
    echo "ANT_HOME not set!"
    exit 1
elif [ ! ${CATALINA_HOME} ]; then
    echo "CATALINA_HOME not set!"
    exit 1
elif [ ! ${CATALINA_BASE_XMLUI_OAI} ]; then
    echo "CATALINA_BASE_XMLUI_OAI not set!"
    exit 1
elif [ ! ${CATALINA_BASE_SOLR} ]; then
    echo "CATALINA_BASE_SOLR not set!"
    exit 1
elif [ ! ${INSTALLER} ]; then
    echo "Could not find any installer. Maybe you should "
    exit 1
elif [ "$(ant -version)" -a $? -ne 0 ]; then
    echo "Could not execute ant. Installed?"
    exit 1
fi
########## /SANITY CHECKS ###################

######## Step 1: Extract ant installer to $SCRIPT_DIR/$EXTRACT_DIR ########
INSTALLER_DIR=`tar -tf ${INSTALLER} | head -n1 | sed -e 's#/$##'`
if [[ ${INSTALLER_DIR:0:1} == '/' ]]
then
    EXTRACT_DIR=${INSTALLER_DIR}
else
    EXTRACT_DIR=${SCRIPT_DIR}/${INSTALLER_DIR}
fi

echo "Extracting ${INSTALLER} into ${EXTRACT_DIR}"
rm -rd ${EXTRACT_DIR}
tar -xzf ${INSTALLER}

if [ $? -ne 0 ]; then
    echo "Error: Decompression of ${INSTALLER} failed"
    exit 1
fi
######## /Step 1: Extract ant installer to $SCRIPT_DIR/$EXTRACT_DIR ########

DSPACE_CFG=`find -name 'local.cfg' -type f`
DSPACE_DIR=`grep dspace.dir ${DSPACE_CFG} | awk -F= '{print $2}' | sed 's/^[ \t]*//;s/[ \t]*$//'`

if [ ! DSPACE_DIR ]; then
    echo "Could not locate dspace_dir in configuration"
    exit 1
fi



DATABASE_USER=`grep db.username ${DSPACE_CFG} | awk -F= '{print $2}' | sed 's/^[ \t]*//;s/[ \t]*$//'`
DATABASE_URL=`grep db.url ${DSPACE_CFG} | awk -F= '{print $2}' | sed 's/^[ \t]*//;s/[ \t]*$//'`
DATABASE_PWD=`grep db.password ${DSPACE_CFG} | awk -F= '{print $2}' | sed 's/^[ \t]*//;s/[ \t]*$//'`
DATABASE_HOST=`echo ${DATABASE_URL} | sed -e 's/^.*\/\/\(.*\)\(:\d*\)\?\/.*$/\1/' | awk -F: '{print $1}'`
DATABASE_PORT=`echo ${DATABASE_URL} | sed -e 's/^.*\/\/\(.*\)\(:\d*\)\?\/.*$/\1/' | awk -F: '{print $2}'`
DATABASE=`echo ${DATABASE_URL} | sed -e 's/.*\/\(.*\)$/\1/'`

#make sure backup folders exists
mkdir -p ${BACKUP_DIR}
mkdir -p ${LATEST_BACKUP_DIR}

#dspace command
DSPACE_CMD=${DSPACE_DIR}/bin/dspace


echo "###################################################################"
echo "Brage configuration:"
echo "Dspace install dir: ${DSPACE_DIR}"
echo "Database host: ${DATABASE_HOST}"
echo "Database port: ${DATABASE_PORT}"
echo "Database: ${DATABASE}"
echo "Database user: ${DATABASE_USER}"

if [ ${DATABASE_PWD} ]; then
    echo "Database password: *****"
else
    echo "Database password not set"
fi
echo "###################################################################"


######## Step 2. Iff latest backup: Roll over $SCRIPT_DIR/backup/latest to $SCRIPT_DIR/backup/brage_<YYYYmmDD>T<HHMMSS>.tar.gz ########
if [ \( -d ${LATEST_BACKUP_DIR} \) -a \( "$(ls -A ${LATEST_DIR})" \) ]; then
    BACKUP_FILE=${BACKUP_DIR}/brage_`date +%Y%m%dT%H%M%S`.tar.gz
    echo "Rolling over ${LATEST_BACKUP_DIR} to ${BACKUP_FILE}"
    tar -zcf ${BACKUP_FILE} -C ${LATEST_BACKUP_DIR} .
	if [ $? -ne 0 ]; then
	    echo "Compression of ${LATEST_BACKUP_DIR} failed"
	    exit
	else
	    echo "Backup of ${LATEST_BACKUP_DIR} successful"
    fi
	echo "Prepare ${LATEST_BACKUP_DIR} for next iteration by removing existing content"
	rm -rd ${LATEST_BACKUP_DIR}/*
else
    echo "No backups found in ${LATEST_BACKUP_DIR}. Skipping backup rotation"
fi
######## / Step 2. Iff latest backup: Roll over $SCRIPT_DIR/backup/latest to $SCRIPT_DIR/backup/brage_<YYYYmmDD>T<HHMMSS>.tar.gz ########

######## Step 3. Iff existing brage installation: $DSPACE_DIR to $SCRIPT_DIR/backup/latest/brage_app.tar.gz ########
if [ \( -d ${DSPACE_DIR} \) -a \( "$(ls -A ${DSPACE_DIR})" \) ]; then
	BACKUP_FILE=${LATEST_BACKUP_DIR}/brage_app.tar.gz
	echo "Backing up ${DSPACE_DIR} to ${BACKUP_FILE} (exclusions [${DSPACE_DIR}/log, ${DSPACE_DIR}/solr/*/data*, ${DSPACE_DIR}/solr-*, ${DSPACE_DIR}/exports])"
	tar -czf ${BACKUP_FILE} ${DSPACE_DIR} --exclude=${DSPACE_DIR}/log --exclude=${DSPACE_DIR}/solr/*/data* --exclude=${DSPACE_DIR}/solr-* --exclude=${DSPACE_DIR}/exports --exclude=${DSPACE_DIR}/solr/solr-export

	if [ $? -ne 0 ]; then
	    echo "Backup of ${DSPACE_DIR} failed"
	    exit 1
	else
	    echo "Backup of ${DSPACE_DIR} successful"
	fi
else
    echo "No files to back up in ${DSPACE_DIR}"
fi
######## / Step 3. Iff existing brage installation: $DSPACE_DIR to $SCRIPT_DIR/backup/latest/brage_app.tar.gz ########

######## 4. Iff existing brage database: Backup $DATABASE to $SCRIPT_DIR/backup/latest/$DATABASE.sql ########
DB_CONNECTION_OPTS="-U ${DATABASE_USER} -h ${DATABASE_HOST}"
if [ ${DATABASE_PORT} ]; then
	DB_CONNECTION_OPTS="${DB_CONNECTION_OPTS} -p ${DATABASE_PORT}"
fi

psql -lqt ${DB_CONNECTION_OPTS} | cut -d \| -f 1 | grep -qw ${DATABASE}
if [ $? -eq 0 ]; then
	BACKUP_FILE=${LATEST_BACKUP_DIR}/${DATABASE}.sql

	echo "Backing up ${DATABASE} on ${DATABASE_HOST} to ${BACKUP_FILE}"
	pg_dump ${DB_CONNECTION_OPTS} ${DATABASE} > ${BACKUP_FILE}
	if [ $? -ne 0 ]; then
	    echo "Backup of ${DATABASE} on ${DATABASE_HOST} failed"
	    exit
	else
	    echo "Backup of ${DATABASE} on ${DATABASE_HOST} to ${BACKUP_FILE} successful"
	fi
else
	echo "Could not find ${DATABASE} on ${DATABASE_HOST}. Nothing to back up"
fi
######## / 4. Iff existing brage database: Backup $DATABASE to $SCRIPT_DIR/backup/latest/<backup>.sql ########

######### 5. Run ant install ($SCRIPT_DIR/$EXTRACT_DIR) ########
echo "Running ant fresh_install"
ant -buildfile ${EXTRACT_DIR}/build.xml fresh_install

if [ $? -ne 0 ]; then
    echo "Could not install brage in ${DSPACE_DIR}"
    exit
fi

echo "Running ant update_configs"
ant -buildfile ${EXTRACT_DIR}/build.xml update_configs

if [ $? -ne 0 ]; then
    echo "Could not update brage configs in ${DSPACE_DIR}"
    exit
fi

######### /5. Run ant install ($SCRIPT_DIR/$EXTRACT_DIR) ########

######### 6. Install BIBSYS scripts ($DSPACE_DIR/${BIBSYS_SCRIPT_DIR}) ########
echo "Installing custom scripts in ${DSPACE_DIR}/${BIBSYS_SCRIPT_DIR}"
cp -R ${SCRIPT_DIR}/${BIBSYS_SCRIPT_DIR} ${DSPACE_DIR}/
echo "Make sure all scripts in ${DSPACE_DIR}/${BIBSYS_SCRIPT_DIR} are executable"
chmod u+x -R ${DSPACE_DIR}/${BIBSYS_SCRIPT_DIR}/*
######### /6. Install BIBSYS scripts ($DSPACE_DIR/${BIBSYS_SCRIPT_DIR}) ########

######## 7. Shutdown tomcat - prepare for new version ########
echo "Shutting down tomcat"
${DSPACE_DIR}/${BIBSYS_SCRIPT_DIR}/brage-shutdown.sh
######## /7. Shutdown tomcat - prepare for new version ########

######## 8. Remove existing webapps from [$CATALINA_BASE_XMLUI_OAI/$CATALINA_BASE_SOLR]/webapps (solr, oai, xmlui) ########
echo "Removing webapps in ${CATALINA_BASE_XMLUI_OAI}/webapps"
echo "Removing ${CATALINA_BASE_XMLUI_OAI}/webapps/oai"
rm -rf ${CATALINA_BASE_XMLUI_OAI}/webapps/oai
echo "Removing ${CATALINA_BASE_XMLUI_OAI}/webapps/xmlui"
rm -rf ${CATALINA_BASE_XMLUI_OAI}/webapps/xmlui

echo "Removing webapps in ${CATALINA_BASE_SOLR}/webapps"
echo "Removing ${CATALINA_BASE_SOLR}/webapps/solr"
rm -rf ${CATALINA_BASE_SOLR}/webapps/solr

######## /8. Remove existing webapps from [$CATALINA_BASE_XMLUI_OAI/$CATALINA_BASE_SOLR]/webapps (solr, oai, xmlui) ########


######## 9. Iff $DATABASE has pending migrations - run $DATABASE migrations ########
if [ "`${DSPACE_CMD} database info | grep -m1 -io pending`" ]; then
    echo "Database has migrations pending"
    echo "Running database migrations. This job may take some time. Coffee...?"
    ${DSPACE_CMD} database migrate
    if [ \( $? -ne 0 \) -o \( "`${DSPACE_CMD} database info | grep -m1 -io pending`" \) ]; then
	    echo "Database migrations failed. Check dspace logs for details"
	    exit 1
    fi
    echo "Finished database migrations"
    REBUILD_INDEX=1
fi
######## /9. Iff $DATABASE has pending migrations - run $DATABASE migrations ########

######## 10. Copy $DSPACE_DIR/webapps to [$CATALINA_BASE_XMLUI_OAI/$CATALINA_BASE_SOLR]/webapps (solr, oai, xmlui) ########
echo "Installing webapps in ${CATALINA_BASE_SOLR}/webapps"
echo "Installing solr (${DSPACE_DIR}/webapps/solr -> ${CATALINA_BASE_SOLR}/webapps/solr)"
cp -R ${DSPACE_DIR}/webapps/solr ${CATALINA_BASE_SOLR}/webapps/

echo "Installing webapps in ${CATALINA_BASE_XMLUI_OAI}/webapps"
echo "Installing oai (${DSPACE_DIR}/webapps/oai -> ${CATALINA_BASE_XMLUI_OAI}/webapps/oai)"
cp -R ${DSPACE_DIR}/webapps/oai ${CATALINA_BASE_XMLUI_OAI}/webapps/

echo "Installing xmlui (${DSPACE_DIR}/webapps/xmlui -> ${CATALINA_BASE_XMLUI_OAI}/webapps/xmlui)"
cp -R ${DSPACE_DIR}/webapps/xmlui ${CATALINA_BASE_XMLUI_OAI}/webapps/
######## /10. Copy $DSPACE_DIR/webapps to [$CATALINA_BASE_XMLUI_OAI/$CATALINA_BASE_SOLR]/webapps (solr, oai, xmlui) ########

######## 11. Create missing top communities (registered in institution-registry.cfg, but missing in database) ########
echo "Create top-communities from institution-registry.cfg..."
${DSPACE_CMD} create-missing-communities
echo "Finished creating top-communities"
######## /11. Create missing top communities (registered in institution-registry.cfg, but missing in database) ########

######## 12. Dynamically map cristin workflow (XML workflow) to cristin collections ########
echo "Mapping cristin workflow (XML workflow) to cristin collections"
${DSPACE_CMD} map-cristin-workflow
echo "Finished mapping cristin workflows"
######## /12. Dynamically map cristin workflow (XML workflow) to cristin collections ########

######## 13. Dynamically map community themes to handles ########
# We need to run this step before tomcat because the mapping functionality does the following:
# - adds additional folders ${CATALINA_BASE_XMLUI_OAI}/webapps/xmlui (does not require restart)
# - changes the runtime configuration xmlui.xconf (dynamic mapping between handles in database and institution-registry, requires restart if there are changes)
# - theme folders are served statically from outside the servlet container. These folders need to be created with execute permission on folders for "others"
echo "Mapping top-community themes to top-community handles..."
${DSPACE_CMD} map-handles-to-themes ${CATALINA_BASE_XMLUI_OAI}/webapps/xmlui

THEMES_DIR=$(find ${CATALINA_BASE_XMLUI_OAI}/webapps/xmlui -maxdepth 2 -type d -name 'themes')

echo "Give 'others' read permission in all subdirs of ${THEMES_DIR} so we can serve them from outside the servlet container"
chmod -R o+r ${THEMES_DIR}

TOP_DIRS=$(echo ${THEMES_DIR} | sed -e "s#${CATALINA_BASE_XMLUI_OAI}##g" | sed -e "s#/# #g")
echo "Set execute permissions on folders on path from ${CATALINA_BASE_XMLUI_OAI} down to ${THEMES_DIR}"

PERMISSION_FOLDER=${CATALINA_BASE_XMLUI_OAI}
for subdir in ${TOP_DIRS}; do
    #Check if we should append / or not
    if $(echo ${THEMES_DIR} | grep -q '/$'); then
	    PERMISSION_FOLDER=${PERMISSION_FOLDER}${subdir}
    else
	    PERMISSION_FOLDER=${PERMISSION_FOLDER}'/'${subdir}
    fi
    echo "Setting execute permission for 'others' on ${PERMISSION_FOLDER}"
    chmod o+x ${PERMISSION_FOLDER}
done

echo "Setting execute persmission for 'others' on all subdirs in ${THEMES_DIR}"
find ${THEMES_DIR} -type d -exec chmod o+x {} \;
echo "Finished mapping themes to top-communities. Restart tomcat"
######## /13. Dynamically map community themes to handles ########

######## 14. Start tomcat - run new version of Brage ########
#Tomcat must run before next step (oai requires solr). Use nohup to prevent tomcat from exiting on hangup
nohup ${DSPACE_DIR}/${BIBSYS_SCRIPT_DIR}/brage-startup.sh </dev/null > build_startup_result.log 2>&1
TOMCAT_STARTUP_RESULT=$?
cat build_startup_result.log
rm build_startup_result.log
if [ ${TOMCAT_STARTUP_RESULT} -ne 0 ]; then
  exit 1
fi
######## /14. Start tomcat - prepare for new version ########

######## 15. Iff migrations - update app index and oai index ########
if [ ${REBUILD_INDEX} ]; then
    echo "Database structure has changed. Rebuilding indexes. This job will take a while. Time to grab some more coffee..."
    ${DSPACE_CMD} index-discovery -b
    echo "Rebuilding oai index"
    ${DSPACE_CMD} oai import -c -o
else
    echo "No changes in database structure. Skipping index rebuild"
fi
######## /15. Iff migrations - update app index and oai index ########

######## 16. Install crontab for user (crontab_template) ########
if [ -f ${SCRIPT_DIR}/crontab_template  ]; then
    echo "Configuring crontab for ${USER}"
    export DSPACE_DIR CATALINA_BASE_XMLUI_OAI CATALINA_BASE_SOLR BIBSYS_SCRIPT_DIR
    envsubst < ${SCRIPT_DIR}/crontab_template | crontab -
    if [ $? -eq 0 ]; then
	    echo "Crontab modified"
    else
	    echo "Crontab modification failed"
    fi
else
    echo "Could not find ${SCRIPT_DIR}/crontab_template. Skipping crontab modification"
fi
######## /16. Install crontab for user (crontab_template) ########
exit
