--
-- database_schema.sql
--
-- Version: $Revision$
--
-- Date:    $Date$
--
-- Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
--
--
--
--   DSpace SQL table definitions
--
--   Authors:   Peter Breton, Robert Tansley, David Stuve, Daniel Chudnov
--
--       This file is used as-is to initialize a database. Therefore,
--       table and view definitions must be ordered correctly.
--

-------------------------------------------------------
-- BitstreamFormatRegistry table
-------------------------------------------------------
DROP TABLE BitstreamFormatRegistry;

CREATE TABLE BitstreamFormatRegistry
(
  bitstream_format_id INTEGER PRIMARY KEY,
  mimetype            VARCHAR(48),
  short_description   VARCHAR(128) UNIQUE,
  description         TEXT,
  support_level       INTEGER,
  -- Identifies internal types
  internal             BOOL
);

-------------------------------------------------------
-- Bitstream table
-------------------------------------------------------
DROP TABLE Bitstream;

CREATE TABLE Bitstream
(
   bitstream_id            INTEGER PRIMARY KEY,
   bitstream_format_id     INTEGER REFERENCES BitstreamFormatRegistry(bitstream_format_id),
   name                    VARCHAR(256),
   size                    INTEGER,
   checksum                VARCHAR(64),
   checksum_algorithm      VARCHAR(32),
   description             TEXT,
   user_format_description TEXT,
   source                  VARCHAR(256),
   internal_id             VARCHAR(256),
   deleted                 BOOL
);

-------------------------------------------------------
-- EPerson table
-------------------------------------------------------
DROP TABLE EPerson;

CREATE TABLE EPerson
(
  eperson_id          INTEGER PRIMARY KEY,
  email               VARCHAR(64) UNIQUE,
  password            VARCHAR(64),
  firstname           VARCHAR(64),
  lastname            VARCHAR(64),
  active              BOOL,
  require_certificate BOOL,
  phone	              VARCHAR(32)
);

-------------------------------------------------------
-- EPersonGroup table
-------------------------------------------------------
DROP TABLE EPersonGroup;

CREATE TABLE EPersonGroup
(
  eperson_group_id INTEGER PRIMARY KEY,
  name             VARCHAR(256) UNIQUE
);

-------------------------------------------------------
-- Item table
-------------------------------------------------------
DROP TABLE Item;

CREATE TABLE Item
(
  item_id      INTEGER PRIMARY KEY,
  submitter_id INTEGER REFERENCES EPerson(eperson_id),
  in_archive   BOOL
);

-------------------------------------------------------
-- Bundle table
-------------------------------------------------------
DROP TABLE Bundle;

CREATE TABLE Bundle
(
  bundle_id          INTEGER PRIMARY KEY,
  mets_bitstream_id  INTEGER REFERENCES Bitstream(bitstream_id)
);

-------------------------------------------------------
-- Item2Bundle table
-------------------------------------------------------
DROP TABLE Item2Bundle;

CREATE TABLE Item2Bundle
(
  id        INTEGER PRIMARY KEY,
  item_id   INTEGER REFERENCES Item(item_id),
  bundle_id INTEGER REFERENCES Bundle(bundle_id)
);

-------------------------------------------------------
-- Bundle2Bitstream table
-------------------------------------------------------
DROP TABLE Bundle2Bitstream;

CREATE TABLE Bundle2Bitstream
(
  id           INTEGER PRIMARY KEY,
  bundle_id    INTEGER REFERENCES Bundle(bundle_id),
  bitstream_id INTEGER REFERENCES Bitstream(bitstream_id)
);

-------------------------------------------------------
-- DCTypeRegistry table
-------------------------------------------------------
DROP TABLE DCTypeRegistry;

CREATE TABLE DCTypeRegistry
(
  dc_type_id INTEGER PRIMARY KEY,
  element    VARCHAR(64),
  qualifier  VARCHAR(64),
  scope_note TEXT,
  UNIQUE(element, qualifier)
);

-------------------------------------------------------
-- DCValue table
-------------------------------------------------------
DROP TABLE DCValue;

CREATE TABLE DCValue
(
  dc_value_id   INTEGER PRIMARY KEY,
  item_id       INTEGER REFERENCES Item(item_id),
  dc_type_id    INTEGER REFERENCES DCTypeRegistry(dc_type_id),
  text_value TEXT,
  text_lang  VARCHAR(24),
  place      INTEGER,
  source_id  INTEGER
);

-- An index for dctypes
DROP INDEX dcvalue_dc_type_id_idx;
CREATE INDEX dcvalue_dc_type_id_idx  on DCValue(dc_type_id);

-------------------------------------------------------
-- Community table
-------------------------------------------------------
DROP TABLE Community;

CREATE TABLE Community
(
  community_id      INTEGER PRIMARY KEY,
  name              VARCHAR(128) UNIQUE,
  short_description VARCHAR(512),
  introductory_text TEXT,
  logo_bitstream_id INTEGER REFERENCES Bitstream(bitstream_id),
  copyright_text    TEXT,
  side_bar_text     TEXT
);

-------------------------------------------------------
-- Collection table
-------------------------------------------------------
DROP TABLE Collection;

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
  reviewers         INTEGER REFERENCES EPersonGroup( eperson_group_id ),
  approvers         INTEGER REFERENCES EPersonGroup( eperson_group_id ),
  editors           INTEGER REFERENCES EPersonGroup( eperson_group_id )
);

-------------------------------------------------------
-- Community2Collection table
-------------------------------------------------------
DROP TABLE Community2Collection;

CREATE TABLE Community2Collection
(
  id             INTEGER PRIMARY KEY,
  community_id   INTEGER REFERENCES Community(community_id),
  collection_id  INTEGER REFERENCES Collection(collection_id)
);

-------------------------------------------------------
-- Collection2Item table
-------------------------------------------------------
DROP TABLE Collection2Item;

CREATE TABLE Collection2Item
(
  id            INTEGER PRIMARY KEY,
  collection_id INTEGER REFERENCES Collection(collection_id),
  item_id       INTEGER REFERENCES Item(item_id)
);
-------------------------------------------------------
-- ResourcePolicy table
-------------------------------------------------------
DROP TABLE ResourcePolicy;

CREATE TABLE ResourcePolicy
(
  policy_id            INTEGER PRIMARY KEY,
  resource_type_id     INTEGER,
  resource_id          INTEGER,
  resource_filter      INTEGER,
  resource_filter_arg  INTEGER,
  action_id            INTEGER,
  policy_statement     VARCHAR(256),
  priority             INTEGER,
  notes                TEXT,
  owner_eperson_id     INTEGER REFERENCES EPerson(eperson_id)
);

-------------------------------------------------------
-- EPersonGroup2EPerson table
-------------------------------------------------------
DROP TABLE EPersonGroup2EPerson;

CREATE TABLE EPersonGroup2EPerson
(
  id               INTEGER PRIMARY KEY,
  eperson_group_id INTEGER REFERENCES EPersonGroup(eperson_group_id),
  eperson_id       INTEGER REFERENCES EPerson(eperson_id)
);

-------------------------------------------------------
-- Handle table
-------------------------------------------------------
DROP TABLE Handle;

CREATE TABLE Handle
(
  handle_id        INTEGER PRIMARY KEY,
  handle           VARCHAR(256) UNIQUE,
  resource_type_id INTEGER,
  resource_id      INTEGER
);

-------------------------------------------------------
--  WorkspaceItem table
-------------------------------------------------------
DROP TABLE WorkspaceItem;

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
  stage_reached     INTEGER
);

-------------------------------------------------------
--  WorkflowItem table
-------------------------------------------------------
DROP TABLE WorkflowItem;

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

-------------------------------------------------------
--  TasklistItem table
-------------------------------------------------------
DROP TABLE TasklistItem;

CREATE TABLE TasklistItem
(
  tasklist_id	INTEGER PRIMARY KEY,
  eperson_id	INTEGER REFERENCES EPerson(eperson_id),
  workflow_id	INTEGER REFERENCES WorkflowItem(workflow_id)
);


-------------------------------------------------------
--  RegistrationData table
-------------------------------------------------------
DROP TABLE RegistrationData;

CREATE TABLE RegistrationData
(
  registrationdata_id   INTEGER PRIMARY KEY,
  eperson_id            INTEGER REFERENCES EPerson(eperson_id),
  token                 VARCHAR(48),
  expires		TIMESTAMP
);


-------------------------------------------------------
--  History table
-------------------------------------------------------
DROP TABLE History;

CREATE TABLE History
(
  history_id           INTEGER PRIMARY KEY,
  -- When it was stored
  creation_date        TIMESTAMP,
  -- A checksum to keep serializations from being stored more than once
  checksum             VARCHAR(32) UNIQUE
);

-------------------------------------------------------
--  HistoryState table
-------------------------------------------------------
DROP TABLE HistoryState;

CREATE TABLE HistoryState
(
  history_state_id           INTEGER PRIMARY KEY,
  object_id                  VARCHAR(64)
);

------------------------------------------------------------
-- Convenience views
------------------------------------------------------------

-------------------------------------------------------
--  Item2Handle view
-------------------------------------------------------
-- Note: DSpaceTypes.ITEM = 2
DROP VIEW Item2Handle;

CREATE VIEW Item2Handle as
select handle_id, handle, resource_id as item_id 
from Handle where resource_type_id = 2
;

-------------------------------------------------------
--  Community2Item view
-------------------------------------------------------
DROP VIEW Community2Item;

CREATE VIEW Community2Item as
SELECT Community2Collection.community_id, Collection2Item.item_id 
FROM Community2Collection, Collection2Item
WHERE Collection2Item.collection_id   = Community2Collection.collection_id
;


------------------------------------------------------------
-- Browse subsystem views
------------------------------------------------------------

-------------------------------------------------------
--  DCResult view
-------------------------------------------------------
DROP VIEW DCResult;

CREATE VIEW DCResult as
SELECT DCValue.*, Item.in_archive, Item.submitter_id
FROM DCValue, Item
WHERE Item.item_id = DCValue.item_id
;

-------------------------------------------------------
--  ItemsByAuthor table
-------------------------------------------------------
DROP TABLE ItemsByAuthor;

CREATE TABLE ItemsByAuthor
(
   items_by_author_id INTEGER PRIMARY KEY,
   item_id            INTEGER REFERENCES Item(item_id),
   author             TEXT
);

-------------------------------------------------------
--  CollectionItemsByAuthor view
-------------------------------------------------------
DROP VIEW CollectionItemsByAuthor;

CREATE VIEW CollectionItemsByAuthor as
SELECT Collection2Item.collection_id, ItemsByAuthor.* 
FROM ItemsByAuthor, Collection2Item
WHERE ItemsByAuthor.item_id = Collection2Item.item_id
;

-------------------------------------------------------
--  CommunityItemsByAuthor view
-------------------------------------------------------
DROP VIEW CommunityItemsByAuthor;

CREATE VIEW CommunityItemsByAuthor as
SELECT Community2Item.community_id, ItemsByAuthor.* 
FROM ItemsByAuthor, Community2Item
WHERE ItemsByAuthor.item_id = Community2Item.item_id
;

----------------------------------------
-- ItemsByTitle table
----------------------------------------

DROP TABLE ItemsByTitle;

CREATE TABLE ItemsByTitle
(
   items_by_title_id  INTEGER PRIMARY KEY,
   item_id            INTEGER REFERENCES Item(item_id),
   title              TEXT,
   sort_title         TEXT
);

-------------------------------------------------------
--  CollectionItemsByTitle view
-------------------------------------------------------
DROP VIEW CollectionItemsByTitle;

CREATE VIEW CollectionItemsByTitle as
SELECT Collection2Item.collection_id, ItemsByTitle.* 
FROM ItemsByTitle, Collection2Item
WHERE ItemsByTitle.item_id = Collection2Item.item_id
;

-------------------------------------------------------
--  CommunityItemsByTitle view
-------------------------------------------------------
DROP VIEW CommunityItemsByTitle;

CREATE VIEW CommunityItemsByTitle as
SELECT Community2Item.community_id, ItemsByTitle.* 
FROM ItemsByTitle, Community2Item
WHERE ItemsByTitle.item_id = Community2Item.item_id
;

-------------------------------------------------------
--  ItemsByDate table
-------------------------------------------------------
DROP TABLE ItemsByDate;

CREATE TABLE ItemsByDate
(
   items_by_date_id   INTEGER PRIMARY KEY,
   item_id            INTEGER REFERENCES Item(item_id),
   date_issued        TEXT
);

-------------------------------------------------------
--  CollectionItemsByDate view
-------------------------------------------------------
DROP VIEW CollectionItemsByDate;

CREATE VIEW CollectionItemsByDate as
SELECT Collection2Item.collection_id, ItemsByDate.* 
FROM ItemsByDate, Collection2Item
WHERE ItemsByDate.item_id = Collection2Item.item_id
;

-------------------------------------------------------
--  CommunityItemsByDate view
-------------------------------------------------------
DROP VIEW CommunityItemsByDate;

CREATE VIEW CommunityItemsByDate as
SELECT Community2Item.community_id, ItemsByDate.* 
FROM ItemsByDate, Community2Item
WHERE ItemsByDate.item_id = Community2Item.item_id
;

-------------------------------------------------------
--  ItemsByDateAccessioned table
-------------------------------------------------------
DROP TABLE ItemsByDateAccessioned;

CREATE TABLE ItemsByDateAccessioned
(
   items_by_date_accessioned_id  INTEGER PRIMARY KEY,
   item_id                       INTEGER REFERENCES Item(item_id),
   date_accessioned              TEXT
);

-------------------------------------------------------
--  CollectionItemsByDateAccessioned view
-------------------------------------------------------
DROP VIEW CollectionItemsByDateAccessioned;

CREATE VIEW CollectionItemsByDateAccessioned as
SELECT Collection2Item.collection_id, ItemsByDateAccessioned.* 
FROM ItemsByDateAccessioned, Collection2Item
WHERE ItemsByDateAccessioned.item_id = Collection2Item.item_id
;

-------------------------------------------------------
--  CommunityItemsByDateAccessioned view
-------------------------------------------------------
DROP VIEW CommunityItemsByDateAccessioned;

CREATE VIEW CommunityItemsByDateAccessioned as
SELECT Community2Item.community_id, ItemsByDateAccessioned.* 
FROM ItemsByDateAccessioned, Community2Item
WHERE ItemsByDateAccessioned.item_id = Community2Item.item_id
;

