--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- CREATE notifyservices table
-----------------------------------------------------------------------------------


CREATE SEQUENCE if NOT EXISTS notifyservices_id_seq;

CREATE TABLE notifyservices (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    url VARCHAR(255),
    ldn_url VARCHAR(255)
);

-----------------------------------------------------------------------------------
-- CREATE notifyservices_inbound_patterns_id_seq table
-----------------------------------------------------------------------------------

CREATE SEQUENCE if NOT EXISTS notifyservices_inbound_patterns_id_seq;

CREATE TABLE notifyservices_inbound_patterns (
    id INTEGER PRIMARY KEY,
    service_id INTEGER REFERENCES notifyservices(id) ON DELETE CASCADE,
    pattern VARCHAR(255),
    constrain_name VARCHAR(255),
    automatic BOOLEAN
);

CREATE INDEX notifyservices_inbound_idx ON notifyservices_inbound_patterns (service_id);

-----------------------------------------------------------------------------------
-- CREATE notifyservices_outbound_patterns table
-----------------------------------------------------------------------------------

CREATE SEQUENCE if NOT EXISTS notifyservices_outbound_patterns_id_seq;

CREATE TABLE notifyservices_outbound_patterns (
    id INTEGER PRIMARY KEY,
    service_id INTEGER REFERENCES notifyservices(id) ON DELETE CASCADE,
    pattern VARCHAR(255),
    constrain_name VARCHAR(255)
);

CREATE INDEX notifyservices_outbound_idx ON notifyservices_outbound_patterns (service_id);

