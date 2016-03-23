#!/usr/bin/env bash
#

pushd .
cd ${DRYAD_CODE_DIR}

rm dspace-api/src/test/java/org/dspace/content/CommunityCollectionIntegrationTest.java
rm dspace-api/src/test/java/org/dspace/content/ItemTest.java
rm dspace-api/src/test/java/org/dspace/content/MetadataSchemaTest.java

rm dspace/modules/api-stats/src/test/java/org/datadryad/journalstatistics/extractor/DataFileCountTest.java
rm dspace/modules/api-stats/src/test/java/org/datadryad/journalstatistics/extractor/DataFileTotalSizeByDateTest.java
rm dspace/modules/api-stats/src/test/java/org/datadryad/journalstatistics/extractor/DataFileTotalSizeTest.java
rm dspace/modules/api-stats/src/test/java/org/datadryad/journalstatistics/extractor/DataPackageCountByDateTest.java
rm dspace/modules/api-stats/src/test/java/org/datadryad/journalstatistics/extractor/DataPackageCountTest.java
rm dspace/modules/api-stats/src/test/java/org/datadryad/journalstatistics/extractor/DataPackageUnpublishedCountTest.java
rm dspace/modules/api-stats/src/test/java/org/datadryad/journalstatistics/extractor/EmbargoedDataFileCountTest.java

rm dspace/modules/api-tests/src/test/java/org/dspace/workflow/AutoApproveBlackoutProcessorTest.java

rm dspace/modules/api/src/test/java/org/datadryad/api/DryadDataFileTest.java
rm dspace/modules/api/src/test/java/org/datadryad/api/DryadDataPackageTest.java

rm dspace/modules/doi/dspace-doi-api/src/test/java/org/dspace/doi/PGDOIDatabaseTest.java

rm dspace/modules/journal-landing/journal-landing-webapp/src/test/java/org/datadryad/api/DryadJournalTest.java

rm dspace/modules/payment-system/payment-api/src/test/java/org/dspace/paymentsystem/PaymentSystemImplTest.java

popd

