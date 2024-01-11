--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- CREATE notifyservice table
-----------------------------------------------------------------------------------


CREATE SEQUENCE if NOT EXISTS notifyservice_id_seq;

CREATE TABLE notifyservice (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    url VARCHAR(255),
    ldn_url VARCHAR(255)
);

-----------------------------------------------------------------------------------
-- CREATE notifyservice_inbound_pattern_id_seq table
-----------------------------------------------------------------------------------

CREATE SEQUENCE if NOT EXISTS notifyservice_inbound_pattern_id_seq;

CREATE TABLE notifyservice_inbound_pattern (
    id INTEGER PRIMARY KEY,
    service_id INTEGER REFERENCES notifyservice(id) ON DELETE CASCADE,
    pattern VARCHAR(255),
    constraint_name VARCHAR(255),
    automatic BOOLEAN
);

CREATE INDEX notifyservice_inbound_idx ON notifyservice_inbound_pattern (service_id);
