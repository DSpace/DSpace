#!/bin/sh

# Copy test directory to local filesystem, outside of source repo, and owned by dryad user
sudo mkdir -p -m 0755 ${DRYAD_TEST_DIR}
sudo mkdir -p -m 0755 ${DRYAD_TEST_DIR}/testData
sudo mkdir -p -m 0755 ${DRYAD_TEST_DIR}/assetstore
sudo chown -R $USER ${DRYAD_TEST_DIR}
cp -L -r $TRAVIS_BUILD_DIR/test/config $DRYAD_TEST_DIR/config
cp -R $TRAVIS_BUILD_DIR/dspace-api/src/test/resources/dspaceFolder/testData/* $DRYAD_TEST_DIR/testData/
cp $TRAVIS_BUILD_DIR/dspace/etc/server/settings.xml $HOME/.m2/

psql -U postgres -c "create user dryad_app with createdb;" -d template1
createdb -U dryad_app dryad_repo
psql -U postgres -c "create user ${PGUSER} with createdb;" -d template1
createdb -U ${PGUSER} ${PGDATABASE}

${DRYAD_CODE_DIR}/test/bin/install-test-database.sh
