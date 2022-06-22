--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create tables for ORCID Queue and History
-----------------------------------------------------------------------------------

CREATE SEQUENCE orcid_queue_id_seq;

CREATE TABLE orcid_queue
(
    id INTEGER NOT NULL,
    owner_id RAW(16) NOT NULL,
    entity_id RAW(16),
    put_code VARCHAR(255),
 	record_type VARCHAR(255),
 	description VARCHAR(255),
 	operation VARCHAR(255),
 	metadata CLOB,
 	attempts INTEGER,
    CONSTRAINT orcid_queue_pkey PRIMARY KEY (id),
    CONSTRAINT orcid_queue_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES item (uuid),
    CONSTRAINT orcid_queue_entity_id_fkey FOREIGN KEY (entity_id) REFERENCES item (uuid)
);

CREATE INDEX orcid_queue_owner_id_index on orcid_queue(owner_id);


CREATE SEQUENCE orcid_history_id_seq;

CREATE TABLE orcid_history
(
    id INTEGER NOT NULL,
    owner_id RAW(16) NOT NULL,
    entity_id RAW(16),
    put_code VARCHAR(255),
    timestamp_last_attempt TIMESTAMP,
	response_message CLOB,
	status INTEGER,
	metadata CLOB,
	operation VARCHAR(255),
	record_type VARCHAR(255),
	description VARCHAR(255),
    CONSTRAINT orcid_history_pkey PRIMARY KEY (id),
    CONSTRAINT orcid_history_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES item (uuid),
    CONSTRAINT orcid_history_entity_id_fkey FOREIGN KEY (entity_id) REFERENCES item (uuid)
);

CREATE INDEX orcid_history_owner_id_index on orcid_history(owner_id);