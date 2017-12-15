START TRANSACTION;

-- Delete from certain tables to avoid duplicate keys
DELETE FROM resourcepolicy;
DELETE FROM epersongroup;
DELETE FROM handle;
DELETE FROM metadatavalue;
DELETE FROM metadatafieldregistry;
DELETE FROM metadataschemaregistry;

-- Import tables
COPY dspaceobject FROM '/tmp/dspace-export/ext_dspaceobject.csv' DELIMITER ',' CSV;
COPY site       FROM '/tmp/dspace-export/ext_site.csv' DELIMITER ',' CSV;
COPY epersongroup         FROM '/tmp/dspace-export/ext_epersongroup.csv' DELIMITER ',' CSV;
COPY group2group          FROM '/tmp/dspace-export/ext_group2group.csv' DELIMITER ',' CSV;
COPY group2groupcache     FROM '/tmp/dspace-export/ext_group2groupcache.csv' DELIMITER ',' CSV;
COPY collection FROM '/tmp/dspace-export/ext_collection.csv' DELIMITER ',' CSV;
COPY community FROM '/tmp/dspace-export/ext_community.csv' DELIMITER ',' CSV;
COPY community2collection FROM '/tmp/dspace-export/ext_community2collection.csv' DELIMITER ',' CSV;
COPY community2community  FROM '/tmp/dspace-export/ext_community2community.csv' DELIMITER ',' CSV;
COPY handle     FROM '/tmp/dspace-export/ext_handle.csv' DELIMITER ',' CSV;
COPY metadataschemaregistry  FROM '/tmp/dspace-export/ext_metadataschemaregistry.csv' DELIMITER ',' CSV;
COPY metadatafieldregistry FROM '/tmp/dspace-export/ext_metadatafieldregistry.csv' DELIMITER ',' CSV;
COPY metadatavalue FROM '/tmp/dspace-export/ext_metadatavalue.csv' DELIMITER ',' CSV;
COPY resourcepolicy FROM '/tmp/dspace-export/ext_resourcepolicy.csv' DELIMITER ',' CSV;

COMMIT;
