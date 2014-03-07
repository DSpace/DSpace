--
-- database_schema_4-5.sql
--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--
-- SQL commands to upgrade the database schema of a live DSpace 4.x
-- to the DSpace 5 database schema.
--
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
--

-------------------------------------------
-- Generalize metadata value table       --
-------------------------------------------

DROP INDEX metadatavalue_item_idx;
DROP INDEX metadatavalue_item_idx2;
DROP VIEW dcvalue;

CREATE TABLE MetadataValue2
(
  metadata_value_id  INTEGER PRIMARY KEY DEFAULT NEXTVAL('metadatavalue_seq'),
  object_type        INTEGER NOT NULL,
  object_id          INTEGER,
  metadata_field_id  INTEGER REFERENCES MetadataFieldRegistry(metadata_field_id),
  text_value         TEXT,
  text_lang          VARCHAR(24),
  place              INTEGER,
  authority          VARCHAR(100),
  confidence         INTEGER DEFAULT -1
);

INSERT INTO MetadataValue2 (metadata_value_id, object_type, object_id,
        metadata_field_id, text_value, text_lang, place, authority, confidence)
    SELECT metadata_value_id, 2, item_id, metadata_field_id, text_value,
        text_lang, place, authority, confidence
        FROM MetadataValue;

DROP TABLE MetadataValue;

ALTER TABLE MetadataValue2 RENAME TO MetadataValue;

CREATE VIEW dcvalue AS
    SELECT MetadataValue.metadata_value_id AS "dc_value_id", MetadataValue.object_id,
        MetadataValue.metadata_field_id AS "dc_type_id", MetadataValue.text_value,
        MetadataValue.text_lang, MetadataValue.place
    FROM MetadataValue, MetadataFieldRegistry
    WHERE MetadataValue.metadata_field_id = MetadataFieldRegistry.metadata_field_id
        AND MetadataFieldRegistry.metadata_schema_id = 1;

CREATE INDEX metadatavalue_object_idx ON MetadataValue(object_type, object_id);
CREATE INDEX metadatavalue_object_idx2 ON MetadataValue(object_type, object_id,metadata_field_id);

------------------------------------------------------
-- Move Community metadata columns to MetadataValue --
------------------------------------------------------

INSERT INTO MetadataValue (object_type, object_id, metadata_field_id, text_value)
    SELECT 4, Community.community_id,
        (SELECT metadata_field_id FROM MetadataFieldRegistry
            where metadata_schema_id = (SELECT metadata_schema_id FROM MetadataSchema where short_id = 'dspace')
                AND element = 'community' and qualifier = 'name'),
        Community.name;
INSERT INTO MetadataValue (object_type, object_id, metadata_field_id, text_value)
    SELECT 4, Community.community_id,
        (SELECT metadata_field_id FROM MetadataFieldRegistry
            where metadata_schema_id = (SELECT metadata_schema_id FROM MetadataSchema where short_id = 'dspace')
                AND element = 'community' and qualifier = 'short_description'),
        Community.short_description;
INSERT INTO MetadataValue (object_type, object_id, metadata_field_id, text_value)
    SELECT 4, Community.community_id,
        (SELECT metadata_field_id FROM MetadataFieldRegistry
            where metadata_schema_id = (SELECT metadata_schema_id FROM MetadataSchema where short_id = 'dspace')
                AND element = 'community' and qualifier = 'introductory_text'),
        Community.introductory_text;
INSERT INTO MetadataValue (object_type, object_id, metadata_field_id, text_value)
    SELECT 4, Community.community_id,
        (SELECT metadata_field_id FROM MetadataFieldRegistry
            where metadata_schema_id = (SELECT metadata_schema_id FROM MetadataSchema where short_id = 'dspace')
                AND element = 'community' and qualifier = 'copyright_text'),
        Community.copyright_text;
INSERT INTO MetadataValue (object_type, object_id, metadata_field_id, text_value)
    SELECT 4, Community.community_id,
        (SELECT metadata_field_id FROM MetadataFieldRegistry
            where metadata_schema_id = (SELECT metadata_schema_id FROM MetadataSchema where short_id = 'dspace')
                AND element = 'community' and qualifier = 'sidebar_text'),
        Community.sidebar_text;

ALTER TABLE Community DROP COLUMN name;
ALTER TABLE Community DROP COLUMN short_description;
ALTER TABLE Community DROP COLUMN introductory_text;
ALTER TABLE Community DROP COLUMN copyright_text;
ALTER TABLE Community DROP COLUMN sidebar_text;

-------------------------------------------------------
-- Move Collection metadata columns to MetadataValue --
-------------------------------------------------------

INSERT INTO MetadataValue (object_type, object_id, metadata_field_id, text_value)
    SELECT 3, Collection.collection_id,
        (SELECT metadata_field_id FROM MetadataFieldRegistry
            where metadata_schema_id = (SELECT metadata_schema_id FROM MetadataSchema where short_id = 'dspace')
                AND element = 'collection' and qualifier = 'name'),
        Collection.name;
INSERT INTO MetadataValue (object_type, object_id, metadata_field_id, text_value)
    SELECT 3, Collection.collection_id,
        (SELECT metadata_field_id FROM MetadataFieldRegistry
            where metadata_schema_id = (SELECT metadata_schema_id FROM MetadataSchema where short_id = 'dspace')
                AND element = 'collection' and qualifier = 'short_description'),
        Collection.short_description;
INSERT INTO MetadataValue (object_type, object_id, metadata_field_id, text_value)
    SELECT 3, Collection.collection_id,
        (SELECT metadata_field_id FROM MetadataFieldRegistry
            where metadata_schema_id = (SELECT metadata_schema_id FROM MetadataSchema where short_id = 'dspace')
                AND element = 'collection' and qualifier = 'introductory_text'),
        Collection.introductory_text;
INSERT INTO MetadataValue (object_type, object_id, metadata_field_id, text_value)
    SELECT 3, Collection.collection_id,
        (SELECT metadata_field_id FROM MetadataFieldRegistry
            where metadata_schema_id = (SELECT metadata_schema_id FROM MetadataSchema where short_id = 'dspace')
                AND element = 'collection' and qualifier = 'provenance'),
        Collection.provenance_description;
INSERT INTO MetadataValue (object_type, object_id, metadata_field_id, text_value)
    SELECT 3, Collection.collection_id,
        (SELECT metadata_field_id FROM MetadataFieldRegistry
            where metadata_schema_id = (SELECT metadata_schema_id FROM MetadataSchema where short_id = 'dspace')
                AND element = 'collection' and qualifier = 'license_text'),
        Collection.license;
INSERT INTO MetadataValue (object_type, object_id, metadata_field_id, text_value)
    SELECT 3, Collection.collection_id,
        (SELECT metadata_field_id FROM MetadataFieldRegistry
            where metadata_schema_id = (SELECT metadata_schema_id FROM MetadataSchema where short_id = 'dspace')
                AND element = 'collection' and qualifier = 'copyright_text'),
        Collection.copyright_text;
INSERT INTO MetadataValue (object_type, object_id, metadata_field_id, text_value)
    SELECT 3, Collection.collection_id,
        (SELECT metadata_field_id FROM MetadataFieldRegistry
            where metadata_schema_id = (SELECT metadata_schema_id FROM MetadataSchema where short_id = 'dspace')
                AND element = 'collection' and qualifier = 'sidebar_text'),
        Collection.sidebar_text;

ALTER TABLE Collection DROP COLUMN name;
ALTER TABLE Collection DROP COLUMN short_description;
ALTER TABLE Collection DROP COLUMN introductory_text;
ALTER TABLE Collection DROP COLUMN provenance_description;
ALTER TABLE Collection DROP COLUMN license;
ALTER TABLE Collection DROP COLUMN copyright_text;
ALTER TABLE Collection DROP COLUMN sidebar_text;
