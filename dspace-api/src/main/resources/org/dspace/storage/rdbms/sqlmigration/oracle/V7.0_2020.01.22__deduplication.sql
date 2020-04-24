--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

CREATE SEQUENCE deduplication_id_seq;

-----------------------------------------------------------------------------------
-- Create deduplication table
-----------------------------------------------------------------------------------
CREATE TABLE deduplication (
    deduplication_id INTEGER PRIMARY KEY,
    fake NUMBER(1),
    tofix NUMBER(1),
    note VARCHAR2(256),
    admin_time TIMESTAMP,
    reader_time TIMESTAMP,
    reader_note VARCHAR2(256),
    reject_time TIMESTAMP,
    submitter_decision VARCHAR2(256),
    workflow_decision VARCHAR2(256),
    admin_decision VARCHAR2(256),
    eperson_id RAW(16),
    admin_id RAW(16),
    reader_id RAW(16),
    first_item_id RAW(16),
    second_item_id RAW(16)
);