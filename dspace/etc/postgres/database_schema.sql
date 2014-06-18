--
-- database_schema.sql
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
--
--
--
--   DSpace SQL schema
--
--   Authors:   Peter Breton, Robert Tansley, David Stuve, Daniel Chudnov,
--              Richard Jones
--
--   This file is used as-is to initialize a database. Therefore,
--   table and view definitions must be ordered correctly.
--
--   Caution: THIS IS POSTGRESQL-SPECIFIC:
--
--   * SEQUENCES are used for automatic ID generation
--   * FUNCTION getnextid used for automatic ID generation
--
--
--   To convert to work with another database, you need to ensure
--   an SQL function 'getnextid', which takes a table name as an
--   argument, will return a safe new ID to use to create a new
--   row in that table.

-------------------------------------------------------
-- Function for obtaining new IDs.
--
--   * The argument is a table name
--   * It returns a new ID safe to use for that table
--
--   The function reads the next value from the sequence
--   'tablename_seq'
-------------------------------------------------------
CREATE FUNCTION getnextid(VARCHAR(40)) RETURNS INTEGER AS
    'SELECT CAST (nextval($1 || ''_seq'') AS INTEGER) AS RESULT;' LANGUAGE SQL;


-------------------------------------------------------
-- Sequences for creating new IDs (primary keys) for
-- tables.  Each table must have a corresponding
-- sequence called 'tablename_seq'.
-------------------------------------------------------
CREATE SEQUENCE bitstreamformatregistry_seq;
CREATE SEQUENCE fileextension_seq;
CREATE SEQUENCE bitstream_seq;
CREATE SEQUENCE eperson_seq;
CREATE SEQUENCE epersongroup_seq;
CREATE SEQUENCE item_seq;
CREATE SEQUENCE bundle_seq;
CREATE SEQUENCE item2bundle_seq;
CREATE SEQUENCE bundle2bitstream_seq;
CREATE SEQUENCE dcvalue_seq;
CREATE SEQUENCE community_seq;
CREATE SEQUENCE collection_seq;
CREATE SEQUENCE community2community_seq;
CREATE SEQUENCE community2collection_seq;
CREATE SEQUENCE collection2item_seq;
CREATE SEQUENCE resourcepolicy_seq;
CREATE SEQUENCE epersongroup2eperson_seq;
CREATE SEQUENCE handle_seq;
CREATE SEQUENCE doi_seq;
CREATE SEQUENCE workspaceitem_seq;
CREATE SEQUENCE workflowitem_seq;
CREATE SEQUENCE tasklistitem_seq;
CREATE SEQUENCE registrationdata_seq;
CREATE SEQUENCE subscription_seq;
CREATE SEQUENCE communities2item_seq;
CREATE SEQUENCE epersongroup2workspaceitem_seq;
CREATE SEQUENCE metadataschemaregistry_seq;
CREATE SEQUENCE metadatafieldregistry_seq;
CREATE SEQUENCE metadatavalue_seq;
CREATE SEQUENCE group2group_seq;
CREATE SEQUENCE group2groupcache_seq;
CREATE SEQUENCE harvested_collection_seq;
CREATE SEQUENCE harvested_item_seq;
CREATE SEQUENCE versionitem_seq;
CREATE SEQUENCE versionhistory_seq;
CREATE SEQUENCE webapp_seq;
CREATE SEQUENCE requestitem_seq;

-------------------------------------------------------
-- BitstreamFormatRegistry table
-------------------------------------------------------
CREATE TABLE BitstreamFormatRegistry
(
  bitstream_format_id INTEGER PRIMARY KEY,
  mimetype            VARCHAR(256),
  short_description   VARCHAR(128) UNIQUE,
  description         TEXT,
  support_level       INTEGER,
  -- Identifies internal types
  internal             BOOL
);

-------------------------------------------------------
-- FileExtension table
-------------------------------------------------------
CREATE TABLE FileExtension
(
  file_extension_id    INTEGER PRIMARY KEY,
  bitstream_format_id  INTEGER REFERENCES BitstreamFormatRegistry(bitstream_format_id),
  extension            VARCHAR(16)
);

CREATE INDEX fe_bitstream_fk_idx ON FileExtension(bitstream_format_id);

-------------------------------------------------------
-- Bitstream table
-------------------------------------------------------
CREATE TABLE Bitstream
(
   bitstream_id            INTEGER PRIMARY KEY,
   bitstream_format_id     INTEGER REFERENCES BitstreamFormatRegistry(bitstream_format_id),
   name                    VARCHAR(256),
   size_bytes              BIGINT,
   checksum                VARCHAR(64),
   checksum_algorithm      VARCHAR(32),
   description             TEXT,
   user_format_description TEXT,
   source                  VARCHAR(256),
   internal_id             VARCHAR(256),
   deleted                 BOOL,
   store_number            INTEGER,
   sequence_id             INTEGER
);

CREATE INDEX bit_bitstream_fk_idx ON Bitstream(bitstream_format_id);

-------------------------------------------------------
-- EPerson table
-------------------------------------------------------
CREATE TABLE EPerson
(
  eperson_id          INTEGER PRIMARY KEY,
  email               VARCHAR(64) UNIQUE,
  password            VARCHAR(128),
  salt                VARCHAR(32),
  digest_algorithm    VARCHAR(16),
  firstname           VARCHAR(64),
  lastname            VARCHAR(64),
  can_log_in          BOOL,
  require_certificate BOOL,
  self_registered     BOOL,
  last_active         TIMESTAMP,
  sub_frequency       INTEGER,
  phone               VARCHAR(32),
  netid               VARCHAR(64),
  language            VARCHAR(64)
);

-- index by email
CREATE INDEX eperson_email_idx ON EPerson(email);

-- index by netid
CREATE INDEX eperson_netid_idx ON EPerson(netid);

-------------------------------------------------------
-- EPersonGroup table
-------------------------------------------------------
CREATE TABLE EPersonGroup
(
  eperson_group_id INTEGER PRIMARY KEY,
  name             VARCHAR(256) UNIQUE
);

------------------------------------------------------
-- Group2Group table, records group membership in other groups
------------------------------------------------------
CREATE TABLE Group2Group
(
  id        INTEGER PRIMARY KEY,
  parent_id INTEGER REFERENCES EPersonGroup(eperson_group_id),
  child_id  INTEGER REFERENCES EPersonGroup(eperson_group_id)
);

CREATE INDEX g2g_parent_fk_idx ON Group2Group(parent_id);
CREATE INDEX g2g_child_fk_idx ON Group2Group(child_id);

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

CREATE INDEX g2gc_parent_fk_idx ON Group2Group(parent_id);
CREATE INDEX g2gc_child_fk_idx ON Group2Group(child_id);

-------------------------------------------------------
-- Item table
-------------------------------------------------------
CREATE TABLE Item
(
  item_id         INTEGER PRIMARY KEY,
  submitter_id    INTEGER REFERENCES EPerson(eperson_id),
  in_archive      BOOL,
  withdrawn       BOOL,
  discoverable    BOOL,
  last_modified   TIMESTAMP WITH TIME ZONE,
  owning_collection INTEGER
);

CREATE INDEX item_submitter_fk_idx ON Item(submitter_id);

-------------------------------------------------------
-- Bundle table
-------------------------------------------------------
CREATE TABLE Bundle
(
  bundle_id          INTEGER PRIMARY KEY,
  name               VARCHAR(16),  -- ORIGINAL | THUMBNAIL | TEXT
  primary_bitstream_id  INTEGER REFERENCES Bitstream(bitstream_id)
);

CREATE INDEX bundle_primary_fk_idx ON Bundle(primary_bitstream_id);

-------------------------------------------------------
-- Item2Bundle table
-------------------------------------------------------
CREATE TABLE Item2Bundle
(
  id        INTEGER PRIMARY KEY,
  item_id   INTEGER REFERENCES Item(item_id),
  bundle_id INTEGER REFERENCES Bundle(bundle_id)
);

-- index by item_id
CREATE INDEX item2bundle_item_idx on Item2Bundle(item_id);

CREATE INDEX item2bundle_bundle_fk_idx ON Item2Bundle(bundle_id);

-------------------------------------------------------
-- Bundle2Bitstream table
-------------------------------------------------------
CREATE TABLE Bundle2Bitstream
(
  id              INTEGER PRIMARY KEY,
  bundle_id       INTEGER REFERENCES Bundle(bundle_id),
  bitstream_id    INTEGER REFERENCES Bitstream(bitstream_id),
  bitstream_order INTEGER
);

-- index by bundle_id
CREATE INDEX bundle2bitstream_bundle_idx ON Bundle2Bitstream(bundle_id);

CREATE INDEX bundle2bitstream_bitstream_fk_idx ON Bundle2Bitstream(bitstream_id);

-------------------------------------------------------
-- Metadata Tables and Sequences
-------------------------------------------------------
CREATE TABLE MetadataSchemaRegistry
(
  metadata_schema_id INTEGER PRIMARY KEY DEFAULT NEXTVAL('metadataschemaregistry_seq'),
  namespace          VARCHAR(256) UNIQUE,
  short_id           VARCHAR(32) UNIQUE
);

CREATE TABLE MetadataFieldRegistry
(
  metadata_field_id   INTEGER PRIMARY KEY DEFAULT NEXTVAL('metadatafieldregistry_seq'),
  metadata_schema_id  INTEGER NOT NULL REFERENCES MetadataSchemaRegistry(metadata_schema_id),
  element             VARCHAR(64),
  qualifier           VARCHAR(64),
  scope_note          TEXT
);

CREATE TABLE MetadataValue
(
  metadata_value_id  INTEGER PRIMARY KEY DEFAULT NEXTVAL('metadatavalue_seq'),
  item_id            INTEGER REFERENCES Item(item_id),
  metadata_field_id  INTEGER REFERENCES MetadataFieldRegistry(metadata_field_id),
  text_value         TEXT,
  text_lang          VARCHAR(24),
  place              INTEGER,
  authority          VARCHAR(100),
  confidence         INTEGER DEFAULT -1
);

-- Create a dcvalue view for backwards compatibilty
CREATE VIEW dcvalue AS
  SELECT MetadataValue.metadata_value_id AS "dc_value_id", MetadataValue.item_id,
    MetadataValue.metadata_field_id AS "dc_type_id", MetadataValue.text_value,
    MetadataValue.text_lang, MetadataValue.place
  FROM MetadataValue, MetadataFieldRegistry
  WHERE MetadataValue.metadata_field_id = MetadataFieldRegistry.metadata_field_id
  AND MetadataFieldRegistry.metadata_schema_id = 1;

-- An index for item_id - almost all access is based on
-- instantiating the item object, which grabs all values
-- related to that item
CREATE INDEX metadatavalue_item_idx ON MetadataValue(item_id);
CREATE INDEX metadatavalue_item_idx2 ON MetadataValue(item_id,metadata_field_id);
CREATE INDEX metadatavalue_field_fk_idx ON MetadataValue(metadata_field_id);
CREATE INDEX metadatafield_schema_idx ON MetadataFieldRegistry(metadata_schema_id);
  
-------------------------------------------------------
-- Community table
-------------------------------------------------------
CREATE TABLE Community
(
  community_id      INTEGER PRIMARY KEY,
  name              VARCHAR(128),
  short_description VARCHAR(512),
  introductory_text TEXT,
  logo_bitstream_id INTEGER REFERENCES Bitstream(bitstream_id),
  copyright_text    TEXT,
  side_bar_text     TEXT,
  admin             INTEGER REFERENCES EPersonGroup( eperson_group_id )
);

CREATE INDEX community_logo_fk_idx ON Community(logo_bitstream_id);
CREATE INDEX community_admin_fk_idx ON Community(admin);

-------------------------------------------------------
-- Collection table
-------------------------------------------------------
CREATE TABLE Collection
(
  collection_id     INTEGER PRIMARY KEY,
  name              VARCHAR(128),
  short_description VARCHAR(512),
  introductory_text TEXT,
  logo_bitstream_id INTEGER REFERENCES Bitstream(bitstream_id),
  template_item_id  INTEGER REFERENCES Item(item_id),
  provenance_description  TEXT,
  license           TEXT,
  copyright_text    TEXT,
  side_bar_text     TEXT,
  workflow_step_1   INTEGER REFERENCES EPersonGroup( eperson_group_id ),
  workflow_step_2   INTEGER REFERENCES EPersonGroup( eperson_group_id ),
  workflow_step_3   INTEGER REFERENCES EPersonGroup( eperson_group_id ),
  submitter         INTEGER REFERENCES EPersonGroup( eperson_group_id ),
  admin             INTEGER REFERENCES EPersonGroup( eperson_group_id )
);

CREATE INDEX collection_logo_fk_idx ON Collection(logo_bitstream_id);
CREATE INDEX collection_template_fk_idx ON Collection(template_item_id);
CREATE INDEX collection_workflow1_fk_idx ON Collection(workflow_step_1);
CREATE INDEX collection_workflow2_fk_idx ON Collection(workflow_step_2);
CREATE INDEX collection_workflow3_fk_idx ON Collection(workflow_step_3);
CREATE INDEX collection_submitter_fk_idx ON Collection(submitter);
CREATE INDEX collection_admin_fk_idx ON Collection(admin);

-------------------------------------------------------
-- Community2Community table
-------------------------------------------------------
CREATE TABLE Community2Community
(
  id             INTEGER PRIMARY KEY,
  parent_comm_id INTEGER REFERENCES Community(community_id),
  child_comm_id  INTEGER,
  CONSTRAINT com2com_child_fk FOREIGN KEY (child_comm_id) REFERENCES Community(community_id) DEFERRABLE
);

CREATE INDEX com2com_parent_fk_idx ON Community2Community(parent_comm_id);
CREATE INDEX com2com_child_fk_idx ON Community2Community(child_comm_id);

-------------------------------------------------------
-- Community2Collection table
-------------------------------------------------------
CREATE TABLE Community2Collection
(
  id             INTEGER PRIMARY KEY,
  community_id   INTEGER REFERENCES Community(community_id),
  collection_id  INTEGER,
  CONSTRAINT comm2coll_collection_fk FOREIGN KEY (collection_id) REFERENCES Collection(collection_id) DEFERRABLE
);

-- Index on community ID
CREATE INDEX Community2Collection_community_id_idx ON Community2Collection(community_id);
-- Index on collection ID
CREATE INDEX Community2Collection_collection_id_idx ON Community2Collection(collection_id);

-------------------------------------------------------
-- Collection2Item table
-------------------------------------------------------
CREATE TABLE Collection2Item
(
  id            INTEGER PRIMARY KEY,
  collection_id INTEGER REFERENCES Collection(collection_id),
  item_id       INTEGER,
  CONSTRAINT coll2item_item_fk FOREIGN KEY (item_id) REFERENCES Item(item_id) DEFERRABLE
);

-- index by collection_id
CREATE INDEX collection2item_collection_idx ON Collection2Item(collection_id);
-- and item_id
CREATE INDEX Collection2Item_item_id_idx ON Collection2Item( item_id );

-------------------------------------------------------
-- ResourcePolicy table
-------------------------------------------------------
CREATE TABLE ResourcePolicy
(
  policy_id            INTEGER PRIMARY KEY,
  resource_type_id     INTEGER,
  resource_id          INTEGER,
  action_id            INTEGER,
  eperson_id           INTEGER REFERENCES EPerson(eperson_id),
  epersongroup_id      INTEGER REFERENCES EPersonGroup(eperson_group_id),
  start_date           DATE,
  end_date             DATE,
  rpname               VARCHAR(30),
  rptype               VARCHAR(30),
  rpdescription        VARCHAR(100)
);

-- index by resource_type,resource_id - all queries by
-- authorization manager are select type=x, id=y, action=z
CREATE INDEX resourcepolicy_type_id_idx ON ResourcePolicy(resource_type_id,resource_id);

CREATE INDEX rp_eperson_fk_idx ON ResourcePolicy(eperson_id);
CREATE INDEX rp_epersongroup_fk_idx ON ResourcePolicy(epersongroup_id);

-------------------------------------------------------
-- EPersonGroup2EPerson table
-------------------------------------------------------
CREATE TABLE EPersonGroup2EPerson
(
  id               INTEGER PRIMARY KEY,
  eperson_group_id INTEGER REFERENCES EPersonGroup(eperson_group_id),
  eperson_id       INTEGER REFERENCES EPerson(eperson_id)
);

-- Index by group ID (used heavily by AuthorizeManager)
CREATE INDEX epersongroup2eperson_group_idx on EPersonGroup2EPerson(eperson_group_id);

CREATE INDEX epg2ep_eperson_fk_idx ON EPersonGroup2EPerson(eperson_id);

-------------------------------------------------------
-- Handle table
-------------------------------------------------------
CREATE TABLE Handle
(
  handle_id        INTEGER PRIMARY KEY,
  handle           VARCHAR(256) UNIQUE,
  resource_type_id INTEGER,
  resource_id      INTEGER
);

-- index by handle, commonly looked up
CREATE INDEX handle_handle_idx ON Handle(handle);
-- index by resource id and resource type id
CREATE INDEX handle_resource_id_and_type_idx ON handle(resource_id, resource_type_id);

-------------------------------------------------------
-- Doi table
-------------------------------------------------------
CREATE TABLE Doi
(
  doi_id           INTEGER PRIMARY KEY,
  doi              VARCHAR(256) UNIQUE,
  resource_type_id INTEGER,
  resource_id      INTEGER,
  status           INTEGER
);

-- index by handle, commonly looked up
CREATE INDEX doi_doi_idx ON Doi(doi);
-- index by resource id and resource type id
CREATE INDEX doi_resource_id_and_type_idx ON Doi(resource_id, resource_type_id);

-------------------------------------------------------
--  WorkspaceItem table
-------------------------------------------------------
CREATE TABLE WorkspaceItem
(
  workspace_item_id INTEGER PRIMARY KEY,
  item_id           INTEGER REFERENCES Item(item_id),
  collection_id     INTEGER REFERENCES Collection(collection_id),
  -- Answers to questions on first page of submit UI
  multiple_titles   BOOL,
  published_before  BOOL,
  multiple_files    BOOL,
  -- How for the user has got in the submit process
  stage_reached     INTEGER,
  page_reached      INTEGER
);

CREATE INDEX workspace_item_fk_idx ON WorkspaceItem(item_id);
CREATE INDEX workspace_coll_fk_idx ON WorkspaceItem(collection_id);

-------------------------------------------------------
--  WorkflowItem table
-------------------------------------------------------
CREATE TABLE WorkflowItem
(
  workflow_id    INTEGER PRIMARY KEY,
  item_id        INTEGER REFERENCES Item(item_id) UNIQUE,
  collection_id  INTEGER REFERENCES Collection(collection_id),
  state          INTEGER,
  owner          INTEGER REFERENCES EPerson(eperson_id),

  -- Answers to questions on first page of submit UI
  multiple_titles       BOOL,
  published_before      BOOL,
  multiple_files        BOOL
  -- Note: stage reached not applicable here - people involved in workflow
  -- can always jump around submission UI

);

CREATE INDEX workflow_item_fk_idx ON WorkflowItem(item_id);
CREATE INDEX workflow_coll_fk_idx ON WorkflowItem(collection_id);
CREATE INDEX workflow_owner_fk_idx ON WorkflowItem(owner);

-------------------------------------------------------
--  TasklistItem table
-------------------------------------------------------
CREATE TABLE TasklistItem
(
  tasklist_id   INTEGER PRIMARY KEY,
  eperson_id    INTEGER REFERENCES EPerson(eperson_id),
  workflow_id   INTEGER REFERENCES WorkflowItem(workflow_id)
);

CREATE INDEX tasklist_eperson_fk_idx ON TasklistItem(eperson_id);
CREATE INDEX tasklist_workflow_fk_idx ON TasklistItem(workflow_id);

-------------------------------------------------------
--  RegistrationData table
-------------------------------------------------------
CREATE TABLE RegistrationData
(
  registrationdata_id   INTEGER PRIMARY KEY,
  email                 VARCHAR(64) UNIQUE,
  token                 VARCHAR(48),
  expires               TIMESTAMP
);


-------------------------------------------------------
--  Subscription table
-------------------------------------------------------
CREATE TABLE Subscription
(
  subscription_id   INTEGER PRIMARY KEY,
  eperson_id        INTEGER REFERENCES EPerson(eperson_id),
  collection_id     INTEGER REFERENCES Collection(collection_id)
);

CREATE INDEX subs_eperson_fk_idx ON Subscription(eperson_id);
CREATE INDEX subs_collection_fk_idx ON Subscription(collection_id);


-------------------------------------------------------------------------------
-- EPersonGroup2WorkspaceItem table
-------------------------------------------------------------------------------

CREATE TABLE epersongroup2workspaceitem
(
  id integer DEFAULT nextval('epersongroup2workspaceitem_seq'),
  eperson_group_id integer REFERENCES EPersonGroup(eperson_group_id),
  workspace_item_id integer REFERENCES WorkspaceItem(workspace_item_id),
  CONSTRAINT epersongroup2item_pkey PRIMARY KEY (id)
);

CREATE INDEX epg2wi_group_fk_idx ON epersongroup2workspaceitem(eperson_group_id);
CREATE INDEX epg2wi_workspace_fk_idx ON epersongroup2workspaceitem(workspace_item_id);

-------------------------------------------------------
--  Communities2Item table
-------------------------------------------------------
CREATE TABLE Communities2Item
(
   id                      INTEGER PRIMARY KEY,
   community_id            INTEGER REFERENCES Community(community_id),
   item_id                 INTEGER REFERENCES Item(item_id)
);

-- Index by item_id for update/re-index
CREATE INDEX Communities2Item_item_id_idx ON Communities2Item( item_id );

CREATE INDEX Comm2Item_community_fk_idx ON Communities2Item( community_id );

-------------------------------------------------------
-- Community2Item view
------------------------------------------------------
CREATE VIEW Community2Item as
SELECT Community2Collection.community_id, Collection2Item.item_id
FROM Community2Collection, Collection2Item
WHERE Collection2Item.collection_id   = Community2Collection.collection_id
;

-------------------------------------------------------------------------
-- Tables to manage cache of item counts for communities and collections
-------------------------------------------------------------------------

CREATE TABLE collection_item_count (
        collection_id INTEGER PRIMARY KEY REFERENCES collection(collection_id),
        count INTEGER
);

CREATE TABLE community_item_count (
        community_id INTEGER PRIMARY KEY REFERENCES community(community_id),
        count INTEGER
);

-------------------------------------------------------
--  Create 'special' groups, for anonymous access
--  and administrators
-------------------------------------------------------
-- We don't use getnextid() for 'anonymous' since the sequences start at '1'
INSERT INTO epersongroup VALUES(0, 'Anonymous');
INSERT INTO epersongroup VALUES(getnextid('epersongroup'), 'Administrator');


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

CREATE INDEX mrc_result_fk_idx ON most_recent_checksum( result );

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

CREATE INDEX ch_result_fk_idx ON checksum_history( result );


-- this will insert into the result code
-- the initial results that should be
-- possible

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



-------------------------------------------------------
-- Create the harvest settings table
-------------------------------------------------------
-- Values used by the OAIHarvester to harvest a collection
-- HarvestInstance is the DAO class for this table

CREATE TABLE harvested_collection
(
    collection_id INTEGER REFERENCES collection(collection_id) ON DELETE CASCADE,
    harvest_type INTEGER,
    oai_source VARCHAR,
    oai_set_id VARCHAR,
    harvest_message VARCHAR,
    metadata_config_id VARCHAR,
    harvest_status INTEGER,
    harvest_start_time TIMESTAMP WITH TIME ZONE,
    last_harvested TIMESTAMP WITH TIME ZONE,
    id INTEGER PRIMARY KEY
);

CREATE INDEX harvested_collection_fk_idx ON harvested_collection(collection_id);


CREATE TABLE harvested_item
(
    item_id INTEGER REFERENCES item(item_id) ON DELETE CASCADE,
    last_harvested TIMESTAMP WITH TIME ZONE,
    oai_id VARCHAR,
    id INTEGER PRIMARY KEY
);

CREATE INDEX harvested_item_fk_idx ON harvested_item(item_id);



CREATE TABLE versionhistory
(
  versionhistory_id INTEGER NOT NULL PRIMARY KEY
);

CREATE TABLE versionitem
(
  versionitem_id INTEGER NOT NULL PRIMARY KEY,
  item_id INTEGER REFERENCES Item(item_id),
  version_number INTEGER,
  eperson_id INTEGER REFERENCES EPerson(eperson_id),
  version_date TIMESTAMP,
  version_summary VARCHAR(255),
  versionhistory_id INTEGER REFERENCES VersionHistory(versionhistory_id)
);

CREATE TABLE Webapp
(
    webapp_id INTEGER NOT NULL PRIMARY KEY,
    AppName VARCHAR(32),
    URL VARCHAR,
    Started TIMESTAMP,
    isUI INTEGER
);

CREATE TABLE requestitem
(
  requestitem_id int4 NOT NULL,
  token varchar(48),
  item_id int4,
  bitstream_id int4,
  allfiles bool,
  request_email varchar(64),
  request_name varchar(64),
  request_date timestamp,
  accept_request bool,
  decision_date timestamp,
  expires timestamp,
  CONSTRAINT requestitem_pkey PRIMARY KEY (requestitem_id),
  CONSTRAINT requestitem_token_key UNIQUE (token)
);
