--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- ===============================================================
-- WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
--
-- DO NOT MANUALLY RUN THIS DATABASE MIGRATION. IT WILL BE EXECUTED
-- AUTOMATICALLY (IF NEEDED) BY "FLYWAY" WHEN YOU STARTUP DSPACE.
-- http://flywaydb.org/
-- ===============================================================

---------------------------------------
-- Update MetadataValue to include CLOB
---------------------------------------

CREATE TABLE MetadataValueTemp
(
  metadata_value_id  INTEGER PRIMARY KEY,
  item_id       INTEGER REFERENCES Item(item_id),
  metadata_field_id  INTEGER REFERENCES MetadataFieldRegistry(metadata_field_id),
  text_value CLOB,
  text_lang  VARCHAR(64),
  place              INTEGER
);

INSERT INTO MetadataValueTemp
SELECT * FROM MetadataValue;

DROP VIEW dcvalue;
DROP TABLE MetadataValue;
ALTER TABLE MetadataValueTemp RENAME TO MetadataValue;

CREATE VIEW dcvalue AS
  SELECT MetadataValue.metadata_value_id AS "dc_value_id", MetadataValue.item_id,
    MetadataValue.metadata_field_id AS "dc_type_id", MetadataValue.text_value,
    MetadataValue.text_lang, MetadataValue.place
  FROM MetadataValue, MetadataFieldRegistry
  WHERE MetadataValue.metadata_field_id = MetadataFieldRegistry.metadata_field_id
  AND MetadataFieldRegistry.metadata_schema_id = 1;

CREATE INDEX metadatavalue_item_idx ON MetadataValue(item_id);
CREATE INDEX metadatavalue_item_idx2 ON MetadataValue(item_id,metadata_field_id);

------------------------------------
-- Update Community to include CLOBs
------------------------------------

CREATE TABLE CommunityTemp
(
  community_id      INTEGER PRIMARY KEY,
  name              VARCHAR2(128),
  short_description VARCHAR2(512),
  introductory_text CLOB,
  logo_bitstream_id INTEGER REFERENCES Bitstream(bitstream_id),
  copyright_text    CLOB,
  side_bar_text     VARCHAR2(2000)
);

INSERT INTO CommunityTemp
SELECT * FROM Community;

DROP TABLE Community CASCADE CONSTRAINTS;
ALTER TABLE CommunityTemp RENAME TO Community;

ALTER TABLE Community2Community ADD CONSTRAINT fk_c2c_parent
FOREIGN KEY (parent_comm_id)
REFERENCES Community (community_id);

ALTER TABLE Community2Community ADD CONSTRAINT fk_c2c_child
FOREIGN KEY (child_comm_id)
REFERENCES Community (community_id);

ALTER TABLE Community2Collection ADD CONSTRAINT fk_c2c_community
FOREIGN KEY (community_id)
REFERENCES Community (community_id);

ALTER TABLE Communities2Item ADD CONSTRAINT fk_c2i_community
FOREIGN KEY (community_id)
REFERENCES Community (community_id);

-------------------------------------
-- Update Collection to include CLOBs
-------------------------------------

CREATE TABLE CollectionTemp
(
  collection_id     INTEGER PRIMARY KEY,
  name              VARCHAR2(128),
  short_description VARCHAR2(512),
  introductory_text CLOB,
  logo_bitstream_id INTEGER REFERENCES Bitstream(bitstream_id),
  template_item_id  INTEGER REFERENCES Item(item_id),
  provenance_description  VARCHAR2(2000),
  license           CLOB,
  copyright_text    CLOB,
  side_bar_text     VARCHAR2(2000),
  workflow_step_1   INTEGER REFERENCES EPersonGroup( eperson_group_id ),
  workflow_step_2   INTEGER REFERENCES EPersonGroup( eperson_group_id ),
  workflow_step_3   INTEGER REFERENCES EPersonGroup( eperson_group_id ),
  submitter         INTEGER REFERENCES EPersonGroup( eperson_group_id ),
  admin             INTEGER REFERENCES EPersonGroup( eperson_group_id )
);

INSERT INTO CollectionTemp
SELECT * FROM Collection;

DROP TABLE Collection CASCADE CONSTRAINTS;
ALTER TABLE CollectionTemp RENAME TO Collection;

ALTER TABLE Community2Collection ADD CONSTRAINT fk_c2c_collection
FOREIGN KEY (collection_id)
REFERENCES Collection (collection_id);

ALTER TABLE Collection2Item ADD CONSTRAINT fk_c2i_collection
FOREIGN KEY (collection_id)
REFERENCES Collection (collection_id);

ALTER TABLE WorkspaceItem ADD CONSTRAINT fk_wsi_collection
FOREIGN KEY (collection_id)
REFERENCES Collection (collection_id);

ALTER TABLE WorkflowItem ADD CONSTRAINT fk_wfi_collection
FOREIGN KEY (collection_id)
REFERENCES Collection (collection_id);

ALTER TABLE Subscription ADD CONSTRAINT fk_subs_collection
FOREIGN KEY (collection_id)
REFERENCES Collection (collection_id);
