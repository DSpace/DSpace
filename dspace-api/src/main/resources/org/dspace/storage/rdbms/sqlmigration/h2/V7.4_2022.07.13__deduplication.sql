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
    fake BOOLEAN,
    tofix BOOLEAN,
    note VARCHAR2(256),
    admin_time TIMESTAMP,
    reader_time TIMESTAMP,
    reader_note VARCHAR2(256),
    reject_time TIMESTAMP,
    submitter_decision VARCHAR2(256),
    workflow_decision VARCHAR2(256),
    admin_decision VARCHAR2(256),
    eperson_id uuid,
    admin_id uuid,
    reader_id uuid,
    first_item_id uuid,
    second_item_id uuid
);