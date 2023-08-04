--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create table for System wide alerts
-----------------------------------------------------------------------------------

CREATE SEQUENCE alert_id_seq;

CREATE TABLE systemwidealert
(
    alert_id        INTEGER NOT NULL PRIMARY KEY,
    message         VARCHAR(512),
    allow_sessions  VARCHAR(64),
    countdown_to    TIMESTAMP,
    active          BOOLEAN
);
