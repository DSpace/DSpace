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
  doi              VARCHAR2(256) UNIQUE,
  resource_type_id INTEGER,
  resource_id      INTEGER,
  status           INTEGER
);

-- index by resource id and resource type id
CREATE INDEX doi_resource_id_type_idx ON doi(resource_id, resource_type_id);

-------------------------------------------
-- Table of running web applications for 'dspace version' --
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
