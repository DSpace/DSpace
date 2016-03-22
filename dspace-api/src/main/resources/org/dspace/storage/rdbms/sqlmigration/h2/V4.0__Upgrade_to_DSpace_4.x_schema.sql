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

-------------------------------------------
-- Ensure that discoverable has a sensible default
-------------------------------------------
update item set discoverable=1 WHERE discoverable IS NULL;

-------------------------------------------
-- Add support for DOIs (table and seq.) --
-------------------------------------------

CREATE TABLE Doi
(
  doi_id           INTEGER PRIMARY KEY,
  doi              VARCHAR2(256) UNIQUE,
  resource_type_id INTEGER,
  resource_id      INTEGER,
  status           INTEGER
);

CREATE SEQUENCE doi_seq;

-- index by resource id and resource type id
CREATE INDEX doi_resource_id_type_idx ON doi(resource_id, resource_type_id);

-------------------------------------------
-- Table of running web applications for 'dspace version' --
-------------------------------------------

CREATE TABLE Webapp
(
    webapp_id INTEGER NOT NULL PRIMARY KEY,
    AppName VARCHAR2(32),
    URL VARCHAR2(1000),
    Started TIMESTAMP,
    isUI NUMBER(1)
);

CREATE SEQUENCE webapp_seq;

-------------------------------------------------------
-- DS-824 RequestItem table
-------------------------------------------------------

CREATE TABLE requestitem
(
  requestitem_id INTEGER NOT NULL,
  token varchar(48),
  item_id INTEGER,
  bitstream_id INTEGER,
  allfiles NUMBER(1),
  request_email VARCHAR2(64),
  request_name VARCHAR2(64),
  request_date TIMESTAMP,
  accept_request NUMBER(1),
  decision_date TIMESTAMP,
  expires TIMESTAMP,
  CONSTRAINT requestitem_pkey PRIMARY KEY (requestitem_id),
  CONSTRAINT requestitem_token_key UNIQUE (token)
);

CREATE SEQUENCE requestitem_seq;

-------------------------------------------------------
-- DS-1655 Disable "Initial Questions" page in Submission UI by default
-------------------------------------------------------
update workspaceitem set multiple_titles=1, published_before=1, multiple_files=1;
update workflowitem set multiple_titles=1, published_before=1, multiple_files=1;

-------------------------------------------------------
-- DS-1811 Removing a collection fails if non-Solr DAO has been used before for item count
-------------------------------------------------------
delete from collection_item_count;
delete from community_item_count;
