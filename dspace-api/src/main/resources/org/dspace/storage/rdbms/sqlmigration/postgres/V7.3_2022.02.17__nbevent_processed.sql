--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

CREATE TABLE nbevent_processed (
  nbevent_id VARCHAR(255) NOT NULL,
  nbevent_timestamp TIMESTAMP NULL,
  eperson_uuid UUID NULL,
  item_uuid UUID NULL,
  CONSTRAINT nbevent_pk PRIMARY KEY (nbevent_id),
  CONSTRAINT eperson_uuid_fkey FOREIGN KEY (eperson_uuid) REFERENCES eperson (uuid),
  CONSTRAINT item_uuid_fkey FOREIGN KEY (item_uuid) REFERENCES item (uuid)
);

CREATE INDEX item_uuid_idx ON nbevent_processed(item_uuid);
