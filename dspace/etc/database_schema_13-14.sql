--
-- database_schema_13-14.sql
--
-- Version: $Revision$
--
-- Date:    $Date$
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
-- SQL commands to upgrade the database schema of a live DSpace 1.3 or 1.3.x
-- to the DSpace 1.4 database schema
-- 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 
-------------------------------------------------------------------------------
-- Sequences for Group within Group feature
-------------------------------------------------------------------------------
CREATE SEQUENCE group2group_seq;
CREATE SEQUENCE group2groupcache_seq;

------------------------------------------------------
-- Group2Group table, records group membership in other groups
------------------------------------------------------
CREATE TABLE Group2Group
(
  id        INTEGER PRIMARY KEY,
  parent_id INTEGER REFERENCES EPersonGroup(eperson_group_id),
  child_id  INTEGER REFERENCES EPersonGroup(eperson_group_id)
);

------------------------------------------------------
-- Group2GroupCache table, is the 'unwound' hierarchy in
-- Group2Group.  It explicitly names every parent child
-- relationship, even with nested groups.  For example,
-- If Group2Group lists B is a child of A and C is a child of B,
-- this table will have entries for parent(A,B), and parent(B,C)
-- AND parent(A,C) so that all of the child groups of A can be
-- looked up in a single simple query
------------------------------------------------------
CREATE TABLE Group2GroupCache
(
  id        INTEGER PRIMARY KEY,
  parent_id INTEGER REFERENCES EPersonGroup(eperson_group_id),
  child_id  INTEGER REFERENCES EPersonGroup(eperson_group_id)
);


-------------------------------------------------------
-- New Metadata Tables and Sequences
-------------------------------------------------------
CREATE SEQUENCE metadataschemaregistry_seq;
CREATE SEQUENCE metadatafieldregistry_seq;
CREATE SEQUENCE metadatavalue_seq;

-- MetadataSchemaRegistry table
CREATE TABLE MetadataSchemaRegistry
(
  metadata_schema_id INTEGER PRIMARY KEY DEFAULT NEXTVAL('metadataschemaregistry_seq'),
  namespace          VARCHAR(256),
  short_id           VARCHAR(32)
);

-- MetadataFieldRegistry table
CREATE TABLE MetadataFieldRegistry
(
  metadata_field_id   INTEGER PRIMARY KEY DEFAULT NEXTVAL('metadatafieldregistry_seq'),
  metadata_schema_id  INTEGER NOT NULL REFERENCES MetadataSchemaRegistry(metadata_schema_id),
  element             VARCHAR(64),
  qualifier           VARCHAR(64),
  scope_note          TEXT
);

-- MetadataValue table
CREATE TABLE MetadataValue
(
  metadata_value_id  INTEGER PRIMARY KEY DEFAULT NEXTVAL('metadatavalue_seq'),
  item_id            INTEGER REFERENCES Item(item_id),
  metadata_field_id  INTEGER REFERENCES MetadataFieldRegistry(metadata_field_id),
  text_value         TEXT,
  text_lang          VARCHAR(24),
  place              INTEGER
);

-- Create the DC schema
INSERT INTO MetadataSchemaRegistry VALUES (1,'http://dublincore.org/documents/dcmi-terms/','dc');

-- Migrate the existing DCTypes into the new metadata field registry
INSERT INTO MetadataFieldRegistry
  (metadata_schema_id, metadata_field_id, element, qualifier, scope_note)
  SELECT '1' AS metadata_schema_id, dc_type_id, element, 
     qualifier, scope_note FROM dctyperegistry;

-- Copy the DCValues into the new MetadataValue table
INSERT INTO MetadataValue (item_id, metadata_field_id, text_value, text_lang, place)
  SELECT item_id, dc_type_id, text_value, text_lang, place FROM dcvalue;
  
ALTER TABLE dcvalue RENAME TO dcvalue_old;
CREATE VIEW dcvalue AS
  SELECT MetadataValue.metadata_value_id AS "dc_value_id", MetadataValue.item_id, 
    MetadataValue.metadata_field_id AS "dc_type_id", MetadataValue.text_value, 
    MetadataValue.text_lang, MetadataValue.place  
  FROM MetadataValue, MetadataFieldRegistry
  WHERE MetadataValue.metadata_field_id = MetadataFieldRegistry.metadata_field_id
  AND MetadataFieldRegistry.metadata_schema_id = 1;

SELECT setval('metadatafieldregistry_seq', max(metadata_field_id)) FROM metadatafieldregistry;
SELECT setval('metadatavalue_seq', max(metadata_value_id)) FROM metadatavalue;
SELECT setval('metadataschemaregistry_seq', max(metadata_schema_id)) FROM metadataschemaregistry;


------------------------------------------------------
-- Bitstream table -- increase capacity of file size
-- column, and bring in line with Oracle schema
------------------------------------------------------
ALTER TABLE bitstream ADD COLUMN size_bytes BIGINT;
UPDATE bitstream SET size_bytes = size;
ALTER TABLE bitstream DROP COLUMN size;
