#
# Makefile for this project
# by LINDAT/CLARIN dev team
#
# Note: if you want to update this makefile, create a copy e.g., makefile.project and
# invoke make with "-f makefile.project" parameter
#
# Why makefile? Repository consists of several applictions and tools which 
# must be controlled on different levels:
# - shibboleth
# - handle server
# - apache, tomcat
# - DSpace 
# - cronjobs
#

SHELL=/bin/bash
#export JAVA_HOME=/usr/lib/jvm/jdk1.7.0
ifeq ("${MAVEN_OPTS}","")
export MAVEN_OPTS=-Xmx6g -Xms1g -XX:+UseG1GC
endif
ifeq ("${MAVEN_OPTS}","")
$(error MAVEN_OPTS not set)
endif

#======================================================
# Basic dirs
#======================================================
 
# Directories and files

BASE				:=$(patsubst %/,%,$(dir $(abspath $(lastword $(MAKEFILE_LIST)))))
PROJECT_DIR			:=$(BASE)/..
DIR_CONF            :=$(PROJECT_DIR)/config
DIR_SUBST           :=$(DIR_CONF)/_substituted

DIR_SOURCE          :=$(PROJECT_DIR)/sources
DIR_BUILD           :=$(DIR_SOURCE)/dspace/target/dspace-installer
DIR_UTILITIES       :=$(DIR_SOURCE)/utilities
DIR_DEPLOY_LOGS     :=$(PROJECT_DIR)/logs

#======================================================
# Include generated variable file 
#======================================================

# either include variable from config/ or from config/dist 

VARIABLE_FILE       :=$(DIR_CONF)/variable.makefile
VARIABLE_FILE_DIST  :=$(DIR_CONF)/variable.makefile.example

ifneq ("$(wildcard $(VARIABLE_FILE))","")
  $(info Using variables from $(VARIABLE_FILE))
  include $(VARIABLE_FILE)
else
  $(info Using variables from $(VARIABLE_FILE_DIST))
  include $(VARIABLE_FILE_DIST)
endif


#======================================================
# Other variables
#======================================================

# LINDAT/CLARIN specific

LOCAL_CONF              :=$(DIR_SOURCE)/local.properties
URL_LINDAT_COMMON_GIT   :=https://github.com/ufal/lindat-common.git

DATABASE_NAME           :=$(shell cd $(BASE) && python setup.lindat.py --get="lr.database" --from="$(LOCAL_CONF)")
UTILITIES_DATABASE_NAME :=$(shell cd $(BASE) && python setup.lindat.py --get="lr.utilities.database" --from="$(LOCAL_CONF)")
DB_PORT			:=$(shell cd $(BASE) && python setup.lindat.py --get="lr.database.port" --from="$(LOCAL_CONF)")
DIR_INSTALLATION        :=$(shell cd $(BASE) && python setup.lindat.py --get="dspace.install.dir" --from="$(LOCAL_CONF)")
DIR_LINDAT_COMMON_THEME :=$(shell cd $(BASE) && python setup.lindat.py --get="lr.common.theme" --from="$(LOCAL_CONF)")

DIR_LINDAT_THEME_IN_WEBAPPS :=$(DIR_INSTALLATION)/webapps/xmlui/themes/UFAL/lib/lindat
DIR_DATABASE_BACKUP         :=$(DIR_INSTALLATION)/../database_backup


# Dspace Configuration

DSPACE_BIN              :=${DIR_INSTALLATION}/bin/dspace
DSPACE_CFG_BASENAME     :=dspace.cfg
DSPACE_CFG              :=$(DIR_SOURCE)/dspace/config/$(DSPACE_CFG_BASENAME)
DSPACE_CFG_EXISTS       :=$(shell (if [ -e $(DSPACE_CFG) ]; then echo 1; else echo 0; fi ))
DSPACE_MVN_PROJECTS     :=-P 'xpdf-mediafilter-support,!dspace-jspui,!dspace-sword-client,!dspace-swordv2,!dspace-sword,!dspace-lni,!dspace-rdf,!dspace-xmlui-mirage2'

MVN_ENV			:=local

MVN_OPTS_BASE	:=-Dassembly.recompressZippedFiles=false -Dassembly.ignorePermissions=true -Denforcer.skip=true -Dfork=true -Denv=$(MVN_ENV)
MVN_OPTS		:=$(MVN_OPTS_BASE) -Dmaven.test.skip=true $(DSPACE_MVN_PROJECTS) -P '!generate-test-env'
MVN_OPTS_TEST	:=$(MVN_OPTS_BASE) -Dmaven.test.skip=false $(DSPACE_MVN_PROJECTS) -B

# makefile specific

MAKE+='--no-print-directory'
GREEN   =\e[0;32m
BLUE    =\e[0;34m
RED     =\e[0;31m
CYAN    =\e[0;36m
NORMAL  =\e[0m

#

DATABASE_RESTORE_DIRECTORY=.
# TODO: from backup.conf
DATABASE_BACKUP_DIRECTORY=/home/backup

# tools

ANT_WITH_CONF:=ant -Doverwrite=true
HIDE_PASSW:=sed '/Password/Id'


#======================================================
# Goals
#======================================================

mvn_help:
	mvn help:system

#======================================================
# setup related
#

install_libs:
	./setup.prerequisites.sh

add_cronjobs:
	(crontab -l ; echo "00 04     * * 1     cd $(BASE); sudo make send_info") | crontab -
	(crontab -l ; echo "00 03     * * *     cd $(BASE); sudo make update_oai") | crontab -
	(crontab -l ; echo "00 02     * * *     cd $(BASE); sudo make update_statistics") | crontab -
	(crontab -l ; echo "00 01     * * *     cd $(BASE); sudo make update_sitemap") | crontab -
	(crontab -l ; echo "30 00     * * *     cd $(BASE); sudo make lift_embargos") | crontab -
	(crontab -l ; echo "30 01     * * *     cd $(BASE); sudo make update_openaire_cache") | crontab -

print_message:
	@echo -e "$(GREEN)-- IMPORTANT INFORMATION  ---------------------------------------------$(NORMAL)"; 
	@echo -e "$(CYAN)SOURCE DIRECTORY = $(DIR_SOURCE)"; 
	@echo "INSTALLATION DIRECTORY = ${DIR_INSTALLATION}"; 
	@echo "LOCAL CONFIG FILE = ${LOCAL_CONF}"; 
	@echo "DATABASE_NAME  = ${DATABASE_NAME}";
	@echo -e "$(GREEN)=======================================================================$(NORMAL)";


#======================================================
# build related - most used
#

new_deploy:     compile \
                fresh_install \
                postinstall

deploy:         compile \
                install \
                postinstall

deploy_guru:    compile \
                install_guru \
                postinstall

#======================================================
# build related
# $@ - name of target
#

# This command cleans all the target directories from source produced by ant.
deploy_logs:
	@mkdir -p $(DIR_DEPLOY_LOGS)

clean_source: deploy_logs
	@rm -f $(DIR_DEPLOY_LOGS)/$@.log
	@cd $(DIR_SOURCE) && mvn $(MVN_OPTS) clean 2>&1 | $(HIDE_PASSW) | tee $(DIR_DEPLOY_LOGS)/$@.log

clean_installation:
	@cd $(DIR_BUILD) && $(ANT_WITH_CONF) clean_backups 2>&1 | $(HIDE_PASSW) | tee  $(DIR_DEPLOY_LOGS)/$@.log
	
compile: deploy_logs
	@echo "COMPILING SOURCE ..."
	@echo "Updating lr.compile.time..."
	@if grep -q "lr\.compile\.time" $(LOCAL_CONF);then sed --follow-symlinks -i'' "s/^lr\.compile\.time=.*/lr.compile.time=`date`/" $(LOCAL_CONF); else echo -e "$(RED)Compile time not updated$(NORMAL)";fi
	@cd $(DIR_SOURCE) && mvn clean package $(MVN_OPTS) 2>&1 | $(HIDE_PASSW) | tee $(DIR_DEPLOY_LOGS)/$@.log
	@echo ""
	@if grep -q "BUILD FAILURE" $(DIR_DEPLOY_LOGS)/compile.log; then \
	        echo -e "$(RED)compile.log contains BUILD FAILURE, exiting$(NORMAL)" && exit -1; fi

tests:
	@echo "UNIT TESTING ..."
	@cd $(DIR_SOURCE) && mvn $(MVN_OPTS_TEST) -Dlog4j.configuration=file:$(DIR_INSTALLATION)/config/log4j-console.properties test

install:        print_message \
                test_databases \
                user_prompt \
                install_dspace

install_guru:   print_message \
                install_dspace

install_dspace: 
	@echo "DEPLOYING LINDAT/CLARIN DSpace ..."
	@cd $(DIR_BUILD) &&  $(ANT_WITH_CONF) update 2>&1 | $(HIDE_PASSW) | tee $(DIR_DEPLOY_LOGS)/$@.log 
	@$(MAKE) clean_installation
	@echo "INSTALLATION SUCCESSFUL ..."

postinstall: update_lindat_common
	rm -rf $(DIR_LINDAT_THEME_IN_WEBAPPS)
	ln -s $(DIR_LINDAT_COMMON_THEME) $(DIR_LINDAT_THEME_IN_WEBAPPS)
	@$(MAKE) grant_rights
	@echo "DEPLOYMENT (INSTALL + POSTINSTALL) SUCCESSFUL"

fresh_install: update_lindat_common
	@echo "INSTALLING FRESH LINDAT/CLARIN DSpace ..."
	@cd $(DIR_BUILD) && $(ANT_WITH_CONF) fresh_install 2>&1 | $(HIDE_PASSW) | tee $(DIR_DEPLOY_LOGS)/$@.log 

# Grant rights to installation
grant_rights:
	sudo chown -R $(TOMCAT_USER):$(TOMCAT_GROUP) $(DIR_INSTALLATION)/
	sudo chmod -R g+w $(DIR_INSTALLATION)/
	
# maven uses prerequisites to check for mvn version
# keep this here for non _guru builds
tools_version_check:
	@cd $(BASE) && python setup.lindat.py --ant-version=1.8
	@cd $(BASE) && python setup.lindat.py --mvn-version=3.0

user_prompt: tools_version_check
	@echo ""
	@echo -e "$(RED)Do you really wish to continue? (for Yes type Yes)$(NORMAL)"
	@python -c 'import sys; sys.exit(-1) if raw_input() != "Yes" else None'



#======================================================
# databased related
#

test_dspace_database: 
	@cd $(DIR_BUILD) && $(ANT_WITH_CONF) -logger org.apache.tools.ant.listener.AnsiColorLogger test_database | $(HIDE_PASSW)

test_utilities_database: 
	@cd $(DIR_INSTALLATION)/bin && JAVA_OPTS="-Dlog4j.configuration=file:$(DIR_INSTALLATION)/config/log4j-console.properties -Xmx1024m -Dfile.encoding=UTF-8" ./dspace dsrun "cz.cuni.mff.ufal.lindat.utilities.HibernateFunctionalityManager" "$(DIR_INSTALLATION)"

test_databases: test_utilities_database test_dspace_database

create_databases:
	@echo "CREATING DSpace DATABASE .."
	sudo -u postgres createdb -p $(DB_PORT) "$(DATABASE_NAME)" --owner $(DSPACE_USER) --encoding "UTF-8" --template=template0
	@echo "CREATING UTILITIES DATABASE ..."
	sudo -u postgres createdb -p $(DB_PORT) "$(UTILITIES_DATABASE_NAME)" --owner $(DSPACE_USER) --encoding "UTF-8" --template=template0
	sudo -u postgres psql --set=utildir="$(DIR_UTILITIES)" -p $(DB_PORT) $(UTILITIES_DATABASE_NAME) < $(DIR_UTILITIES)/utilities.sql
	@echo "DONE"

#update utilities database
#dspace5+ db updated through flywaydb only!
update_databases:
	@echo "UPDATING UTILITIES DATABASE ..."
	@echo "RUN e.g., sudo -u postgres psql -p $(DB_PORT) $(UTILITIES_DATABASE_NAME) < ../sources/utilities/licenses.2014.12.17.sql"
	@echo DONE

update_metadata_scheme:
	@echo "Updating metadata scheme with local scheme"
	cd ${DIR_INSTALLATION}/bin; ./dspace registry-loader -metadata ../config/registries/local-types.xml

restore_database:
	@echo "RESTORING DATABASE $(DATABASE_NAME) FROM $(RESTORE_FROM_DATABASE) ..."
	@sudo -u postgres $(pg_dump) -p $(DB_PORT) --clean "$(RESTORE_FROM_DATABASE)" | sudo -u postgres psql -p $(DB_PORT) "$(DATABASE_NAME)" > $(DIR_DEPLOY_LOGS)/$@.log
	@echo "DONE"


#======================================================
# build related - advanced
#

update_code: 
	@echo "UPDATING CODE ..."
	@cd $(DIR_BUILD) &&  $(ANT_WITH_CONF) update_code 2>&1 | $(HIDE_PASSW) | tee $(DIR_DEPLOY_LOGS)/$@.log 
	@echo "SUCCESSFUL"

update_config: 
	@echo "UPDATING CONFIG ..."
	@cd $(DIR_BUILD) &&  $(ANT_WITH_CONF) update_configs 2>&1 | $(HIDE_PASSW) | tee $(DIR_DEPLOY_LOGS)/$@.log 
	@echo "SUCCESSFUL"


#======================================================
# lindat theme related
#

check_lindat_common:
	@echo "Checking lindat common theme into $(DIR_LINDAT_COMMON_THEME)... "
	@test -d $(DIR_LINDAT_COMMON_THEME)/.git || git clone $(URL_LINDAT_COMMON_GIT) --branch releases $(DIR_LINDAT_COMMON_THEME)

update_lindat_common: check_lindat_common
	cd $(DIR_LINDAT_COMMON_THEME) && $(LINDAT_COMMON_THEME_FETCH)


#======================================================
# webserver&co. related
#

# Command starts all running services to provide webserver (to fully $(MAKE) them run)
start:  
	@if [[ -f start_stack.sh ]]; then \
            bash start_stack.sh; \
        elif [[ -f start_stack_example.sh ]]; then \
            bash start_stack_example.sh; \
        else \
            echo "Nothing to start, do you have start_stack.sh?"; \
        fi

# Command stops all running services to provide webserver (to fully dislocate them)
stop:	
	@if [[ -f stop_stack.sh ]]; then \
            bash stop_stack.sh; \
        elif [[ -f stop_stack_example.sh ]]; then \
            bash stop_stack_example.sh; \
        else \
            echo "Nothing to stop, do you have stop_stack.sh?"; \
        fi

# This command just restart all the services
restart:
	@$(MAKE) stop
	@sleep 3 ;
	@$(MAKE) start

#======================================================
# dspace helpers
#

# This command inits statistics that has to be updated to be viewed correctly
init_statistics:
	$(DSPACE_BIN) stat-initial
	$(DSPACE_BIN) stat-report-initial

# This command updates statistics that has to be updated to be viewed correctly
update_statistics:
	$(DSPACE_BIN) stat-general
	$(DSPACE_BIN) stat-monthly
	$(DSPACE_BIN) stat-report-general
	$(DSPACE_BIN) stat-report-monthly

# This command updates the sitemap
update_sitemap:
	$(DSPACE_BIN) generate-sitemaps -a

lift_embargos:
	$(DSPACE_BIN) embargo-lifter -v

# This command creates and sends repository report
send_info:
	$(DSPACE_BIN) healthcheck -e info.recipient

update_oai:
	LC_ALL=en_US.UTF-8 $(DSPACE_BIN) oai import

# Rebuild oai solr from scratch
rebuild_oai:
	LC_ALL=en_US.UTF-8 $(DSPACE_BIN) oai import -c 

update_discovery:
	@echo 'Updating search & browse indicies (solr) ...'
	$(DSPACE_BIN) index-discovery

generate_handle_list:
	@sudo -u $(DSPACE_USER) psql -p $(DB_PORT) $(DATABASE_NAME) -c "select handle from handle where resource_id in (select item_id from item where in_archive = 't' and withdrawn = 'f');" | tail -n +3 | head -n -2

#TODO make this cron friendly
update_openaire_cache:
	$(DIR_INSTALLATION)/bin/openaire-refresh-list


#======================================================
# backups
#

substitute_backup_config:
	@cd $(BASE) && python setup.lindat.py \
	    --substitute="$(DIR_CONF)/backup2l.conf.example" \
	    --substitute-alternative="$(DIR_CONF)/backup2l.conf" \
	    --with="$(LOCAL_CONF)" \
	    --to="$(DIR_SUBST)/backup2l.conf.substituted" 2>&1

# This command makes backup for databases and put it into backuped directory $(DIR_DATABASE_BACKUP)
backup_databases:
	su postgres -l -c '$(pg_dump) -p $(DB_PORT) $(DATABASE_NAME) > $(DIRECTORY_POSTGRESQL)/$(DATABASE_NAME)'
	su postgres -l -c '$(pg_dump) -p $(DB_PORT) $(UTILITIES_DATABASE_NAME) > $(DIRECTORY_POSTGRESQL)/$(UTILITIES_DATABASE_NAME)'
	# with timestamp
	cp $(DIRECTORY_POSTGRESQL)/$(DATABASE_NAME) $(DIR_DATABASE_BACKUP)/$(DATABASE_NAME).dumped.`date +"%d-%m-%y.at.%T"`
	cp $(DIRECTORY_POSTGRESQL)/$(UTILITIES_DATABASE_NAME) $(DIR_DATABASE_BACKUP)/$(UTILITIES_DATABASE_NAME).dumped.`date +"%d-%m-%y.at.%T"`
	# latest one
	mv $(DIRECTORY_POSTGRESQL)/$(DATABASE_NAME) $(DIR_DATABASE_BACKUP)/$(DATABASE_NAME)
	mv $(DIRECTORY_POSTGRESQL)/$(UTILITIES_DATABASE_NAME) $(DIR_DATABASE_BACKUP)/$(UTILITIES_DATABASE_NAME)

# This is core backuping command. Here we have all the backups of all the key configurations.
backup_dspace: backup_databases substitute_backup_config
	sudo mkdir -p $(DATABASE_BACKUP_DIRECTORY)
	$(BACKUP2l) -c $(DIR_SUBST)/backup2l.conf.substituted -b >> $(DIR_DEPLOY_LOGS)/$@.log
	@echo 'Done backuping.'

restore_backup: substitute_backup_config
	sudo mkdir -p $(DATABASE_RESTORE_DIRECTORY)
	@echo 'Restoring from backup ...'
	cd $(DATABASE_RESTORE_DIRECTORY) && $(BACKUP2l) -c $(DIR_BACKUP_CONFIGURATION)/backup2l.conf.substituted -r >> $(DIR_DEPLOY_LOGS)/$@.log
	@echo 'Done'


#======================================================
# tests
#

#TODO no tests present 
#selenium_tests:
#	@cd $(BASE)/../sources/tests; export DISPLAY=:99; /etc/init.d/xvfb start; ant
#	@/etc/init.d/xvfb stop

# migrate database to new version
# this assumes old version runs on default port 5432 and new on 5433
migrate_databases:
	./migrate_db.sh $(DATABASE_NAME)
	./migrate_db.sh $(UTILITIES_DATABASE_NAME)

generate_piwik_reports:
	LC_ALL=en_US.UTF-8 $(DSPACE_BIN) piwik-report-generator
