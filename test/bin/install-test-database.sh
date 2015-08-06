#!/bin/sh

# Load database schema
psql < ${DRYAD_CODE_DIR}/dspace/etc/postgres/database_schema.sql
psql < ${DRYAD_CODE_DIR}/dspace/etc/postgres/database_create_doi_table.sql
psql < ${DRYAD_CODE_DIR}/dspace/etc/postgres/database_change_payment_system.sql
psql < ${DRYAD_CODE_DIR}/dspace/etc/postgres/database_terms_and_condition.sql
psql < ${DRYAD_CODE_DIR}/dspace/etc/collection-workflow-changes.sql
psql < ${DRYAD_CODE_DIR}/dspace/etc/atmire-versioning-changes.sql
psql < ${DRYAD_CODE_DIR}/dspace/etc/postgres/database_dryad_groups_collections.sql
psql < ${DRYAD_CODE_DIR}/dspace/etc/postgres/database_dryad_registries.sql
psql < ${DRYAD_CODE_DIR}/dspace/etc/postgres/dryad-rest-webapp.sql
psql < ${DRYAD_CODE_DIR}/test/etc/postgres/test-system-curator.sql
psql < ${DRYAD_CODE_DIR}/dspace/etc/postgres/update-sequences.sql
psql < ${DRYAD_CODE_DIR}/test/etc/postgres/test-journal-landing.sql

# These are needed somehow by bagit ingest tests
cp -p -r "${DRYAD_CODE_DIR}/dspace/config/crosswalks" "${DRYAD_TEST_DIR}/config/"
cp -p -r "${DRYAD_CODE_DIR}/dspace/config/dspace-solr-search.cfg" "${DRYAD_TEST_DIR}/config/"

# Update the testing version of dspace.cfg with database credentials
# Ansible provisioning installs password into .pgpass, so fish it out of there

cat "${DRYAD_CODE_DIR}/dspace/config/dspace.cfg" \
  | sed "s|db.password = .*|db.password = ${PGPASSWORD}|g" \
  | sed "s|db.username = .*|db.username = ${PGUSER}|g" \
  | sed "s|db.url =.*|db.url = jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}|g" \
  | sed "s|dspace.dir = .*|dspace.dir = ${DRYAD_TEST_DIR}|g" \
  | sed "s|doi.service.testmode= .*|doi.service.testmode= true|g" \
  | sed "s|doi.datacite.connected = .*|doi.datacite.connected = false|g" \
  | sed "s|dspace.url = .*|dspace.url = http://localhost:9999|g" \
  >"${DRYAD_TEST_DIR}/config/dspace.cfg"
