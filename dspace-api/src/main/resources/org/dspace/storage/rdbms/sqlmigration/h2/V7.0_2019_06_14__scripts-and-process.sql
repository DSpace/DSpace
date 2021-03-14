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
CREATE SEQUENCE process_id_seq;

CREATE TABLE process
(
    process_id              INTEGER NOT NULL PRIMARY KEY,
    user_id                 UUID NOT NULL,
    start_time              TIMESTAMP,
    finished_time           TIMESTAMP,
    creation_time           TIMESTAMP NOT NULL,
    script                  VARCHAR(256) NOT NULL,
    status                  VARCHAR(32),
    parameters              VARCHAR(512)
);

CREATE TABLE process2bitstream
(
  process_id   INTEGER REFERENCES process(process_id),
  bitstream_id UUID REFERENCES bitstream(uuid),
  CONSTRAINT PK_process2bitstream PRIMARY KEY (process_id, bitstream_id)
);

CREATE INDEX process_user_id_idx ON process(user_id);
CREATE INDEX process_status_idx ON process(status);
CREATE INDEX process_name_idx on process(script);
CREATE INDEX process_start_time_idx on process(start_time);