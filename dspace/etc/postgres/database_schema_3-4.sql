--
-- database_schema_18-3.sql
--
-- Version: $Revision$
--
-- Date:    $Date: 2012-05-29
--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--
-- SQL commands to upgrade the database schema of a live DSpace 1.8 or 1.8.x
-- to the DSpace 3 database schema
--
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
--

-------------------------------------------
-- Add support for DOIs (table and seq.) --
-------------------------------------------

CREATE SEQUENCE doi_seq;

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

-------------------------------------------
-- DS-1456 table of currently running webapps
-------------------------------------------

CREATE SEQUENCE webapp_seq;

CREATE TABLE Webapp
(
    webapp_id INTEGER NOT NULL PRIMARY KEY,
    AppName VARCHAR(32),
    URL VARCHAR,
    Started TIMESTAMP,
    isUI INTEGER
);


-------------------------------------------------------
-- DS-824 RequestItem table
-------------------------------------------------------

CREATE SEQUENCE requestitem_seq;

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

-------------------------------------------------------
-- DS-1655 Disable "Initial Questions" page in Submission UI by default
-------------------------------------------------------
update workspaceitem set multiple_titles=true, published_before=true, multiple_files=true;
update workflowitem set multiple_titles=true, published_before=true, multiple_files=true;

-------------------------------------------------------
-- DS-1811 Removing a collection fails if non-Solr DAO has been used before for item count
-------------------------------------------------------
delete from collection_item_count;
delete from community_item_count;