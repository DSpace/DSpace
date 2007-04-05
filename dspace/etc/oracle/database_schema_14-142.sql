--
-- database_schema_14-142.sql
--
-- Version: $Revision: 1.0 $
--
-- Date:    $Date: 2007/02/21 06:06:55 $
--
-- Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
-- Institute of Technology.  All rights reserved.
-- 
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are
-- met:
-- 
-- - Redistributions of source code must retain the above copyright
-- notice, this list of conditions and the following disclaimer.
-- 
-- - Redistributions in binary form must reproduce the above copyright
-- notice, this list of conditions and the following disclaimer in the
-- documentation and/or other materials provided with the distribution.
-- 
-- - Neither the name of the Hewlett-Packard Company nor the name of the
-- Massachusetts Institute of Technology nor the names of their
-- contributors may be used to endorse or promote products derived from
-- this software without specific prior written permission.
-- 
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
-- ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
-- LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
-- A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
-- HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
-- INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
-- BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
-- OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
-- ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
-- TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
-- USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
-- DAMAGE.

--
-- SQL commands to upgrade the database schema of a live DSpace 1.4 or 1.4.1
-- to the DSpace 1.4.2 database schema
-- 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 

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
REFERENCING Community (community_id);

ALTER TABLE Community2Community ADD CONSTRAINT fk_c2c_child
FOREIGN KEY (child_comm_id)
REFERENCING Community (community_id);

ALTER TABLE Community2Collection ADD CONSTRAINT fk_c2c_community
FOREIGN KEY (community_id)
REFERENCING Community (community_id);

ALTER TABLE Communities2Item ADD CONSTRAINT fk_c2i_community
FOREIGN KEY (community_id)
REFERENCING Community (community_id);

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
REFERENCING Collection (collection_id);

ALTER TABLE Collection2Item ADD CONSTRAINT fk_c2i_collection
FOREIGN KEY (collection_id)
REFERENCING Collection (collection_id);

ALTER TABLE WorkspaceItem ADD CONSTRAINT fk_wsi_collection
FOREIGN KEY (collection_id)
REFERENCING Collection (collection_id);

ALTER TABLE WorkflowItem ADD CONSTRAINT fk_wfi_collection
FOREIGN KEY (collection_id)
REFERENCING Collection (collection_id);

ALTER TABLE Subscription ADD CONSTRAINT fk_subs_collection
FOREIGN KEY (collection_id)
REFERENCING Collection (collection_id);
