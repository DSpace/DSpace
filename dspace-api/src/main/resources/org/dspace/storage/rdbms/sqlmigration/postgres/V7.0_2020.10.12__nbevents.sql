--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

CREATE TABLE nbevent (
  nbevent_id VARCHAR(255) NOT NULL,
  nbevent_timestamp TIMESTAMP NULL,
  eperson_uuid VARCHAR(45) NULL,
  item_uuid VARCHAR(45) NULL,
  CONSTRAINT nbevent_pk PRIMARY KEY (nbevent_id)
);

CREATE INDEX item_uuid_idx ON nbevent(item_uuid);
