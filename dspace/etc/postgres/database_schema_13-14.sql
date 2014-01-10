--
-- database_schema_13-14.sql
--
-- Version: $Revision$
--
-- Date:    $Date$
--
-- Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
-- - Neither the name of the DSpace Foundation nor the names of its
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
  namespace          VARCHAR(256) UNIQUE,
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

-- Create the Metadata table indexes
CREATE INDEX metadatavalue_item_idx ON MetadataValue(item_id);
CREATE INDEX metadatavalue_item_idx2 ON MetadataValue(item_id,metadata_field_id);
CREATE INDEX metadatafield_schema_idx ON MetadataFieldRegistry(metadata_schema_id);

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
  
DROP TABLE dcvalue;
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

DROP TABLE dctyperegistry;

------------------------------------------------------
-- Bitstream table -- increase capacity of file size
-- column, and bring in line with Oracle schema
------------------------------------------------------
ALTER TABLE bitstream ADD COLUMN size_bytes BIGINT;
UPDATE bitstream SET size_bytes = size;
ALTER TABLE bitstream DROP COLUMN size;

-------------------------------------------------------
-- Create the checksum checker tables
-------------------------------------------------------
-- list of the possible results as determined
-- by the system or an administrator

CREATE TABLE checksum_results
(
    result_code VARCHAR PRIMARY KEY,
    result_description VARCHAR
);


-- This table has a one-to-one relationship
-- with the bitstream table. A row will be inserted
-- every time a row is inserted into the bitstream table, and
-- that row will be updated every time the checksum is
-- re-calculated.

CREATE TABLE most_recent_checksum 
(
    bitstream_id INTEGER PRIMARY KEY REFERENCES bitstream(bitstream_id),
    to_be_processed BOOLEAN NOT NULL,
    expected_checksum VARCHAR NOT NULL,
    current_checksum VARCHAR NOT NULL,
    last_process_start_date TIMESTAMP NOT NULL,
    last_process_end_date TIMESTAMP NOT NULL,
    checksum_algorithm VARCHAR NOT NULL,
    matched_prev_checksum BOOLEAN NOT NULL,
    result VARCHAR REFERENCES checksum_results(result_code)
);


-- A row will be inserted into this table every
-- time a checksum is re-calculated.

CREATE TABLE checksum_history 
(
    check_id BIGSERIAL PRIMARY KEY,  
    bitstream_id INTEGER,
    process_start_date TIMESTAMP,
    process_end_date TIMESTAMP,
    checksum_expected VARCHAR,
    checksum_calculated VARCHAR,
    result VARCHAR REFERENCES checksum_results(result_code)
);

-- this will insert into the result code
-- the initial results 

insert into checksum_results
values
( 
    'INVALID_HISTORY',
    'Install of the cheksum checking code do not consider this history as valid' 
);

insert into checksum_results
values
( 
    'BITSTREAM_NOT_FOUND',
    'The bitstream could not be found' 
);

insert into checksum_results
values
( 
    'CHECKSUM_MATCH',
    'Current checksum matched previous checksum' 
);

insert into checksum_results
values
(
    'CHECKSUM_NO_MATCH',
    'Current checksum does not match previous checksum' 
);

insert into checksum_results
values
( 
    'CHECKSUM_PREV_NOT_FOUND',
    'Previous checksum was not found: no comparison possible' 
);

insert into checksum_results
values
( 
    'BITSTREAM_INFO_NOT_FOUND',
    'Bitstream info not found' 
);

insert into checksum_results
values
( 
    'CHECKSUM_ALGORITHM_INVALID',
    'Invalid checksum algorithm' 
);
insert into checksum_results
values
( 
    'BITSTREAM_NOT_PROCESSED',
    'Bitstream marked to_be_processed=false' 
);
insert into checksum_results
values
( 
    'BITSTREAM_MARKED_DELETED',
    'Bitstream marked deleted in bitstream table' 
);

-- this will insert into the most recent checksum
-- on install all existing bitstreams
-- setting all bitstreams already set as 
-- deleted to not be processed

insert into most_recent_checksum 
(
    bitstream_id,  
    to_be_processed,
    expected_checksum,
    current_checksum,
    last_process_start_date,
    last_process_end_date,
    checksum_algorithm,
    matched_prev_checksum
)
select 
    bitstream.bitstream_id, 
    true, 
    CASE WHEN bitstream.checksum IS NULL THEN '' ELSE bitstream.checksum END, 
    CASE WHEN bitstream.checksum IS NULL THEN '' ELSE bitstream.checksum END, 
    date_trunc('milliseconds', now()),
    date_trunc('milliseconds', now()),
    CASE WHEN bitstream.checksum_algorithm IS NULL THEN 'MD5' ELSE bitstream.checksum_algorithm END,
    true
from bitstream; 

-- Update all the deleted checksums
-- to not be checked
-- because they have since been
-- deleted from the system

update most_recent_checksum
set to_be_processed = false
where most_recent_checksum.bitstream_id in (
select bitstream_id
from bitstream where deleted = true );

-- this will insert into history table
-- for the initial start 
-- we want to tell the users to disregard the initial
-- inserts into the checksum history table

insert into checksum_history
(
    bitstream_id,
    process_start_date,
    process_end_date,
    checksum_expected,
    checksum_calculated
)
select most_recent_checksum.bitstream_id,
     most_recent_checksum.last_process_end_date,
     date_trunc('milliseconds', now()),
      most_recent_checksum.expected_checksum,
      most_recent_checksum.expected_checksum
from most_recent_checksum;

-- update the history to indicate that this was 
-- the first time the software was installed
update checksum_history 
set result = 'INVALID_HISTORY';  

------------------------------------------------------
-- Drop unique community name constraint
-- 
-- FIXME:  Needs testing; the constraint name is not 
-- guaranteed to be the same as below.  This step may
-- need to be performed by hand.
------------------------------------------------------
ALTER TABLE community DROP CONSTRAINT community_name_key;


-------------------------------------------------------
-- Table and views for 'browse by subject' functionality
-------------------------------------------------------
CREATE SEQUENCE itemsbysubject_seq;

-------------------------------------------------------
--  ItemsBySubject table
-------------------------------------------------------
CREATE TABLE ItemsBySubject
(
   items_by_subject_id INTEGER PRIMARY KEY,
   item_id             INTEGER REFERENCES Item(item_id),
   subject             TEXT,
   sort_subject        TEXT
);

-- index by sort_subject
CREATE INDEX sort_subject_idx on ItemsBySubject(sort_subject);

-------------------------------------------------------
--  CollectionItemsBySubject view
-------------------------------------------------------
CREATE VIEW CollectionItemsBySubject as
SELECT Collection2Item.collection_id, ItemsBySubject.* 
FROM ItemsBySubject, Collection2Item
WHERE ItemsBySubject.item_id = Collection2Item.item_id
;

-------------------------------------------------------
--  CommunityItemsBySubject view
-------------------------------------------------------
CREATE VIEW CommunityItemsBySubject as
SELECT Communities2Item.community_id, ItemsBySubject.* 
FROM ItemsBySubject, Communities2Item
WHERE ItemsBySubject.item_id = Communities2Item.item_id
;