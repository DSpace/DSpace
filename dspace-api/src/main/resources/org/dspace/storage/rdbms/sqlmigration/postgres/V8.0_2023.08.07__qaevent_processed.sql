--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

CREATE TABLE qaevent_processed (
  qaevent_id VARCHAR(255) NOT NULL,
  qaevent_timestamp TIMESTAMP NULL,
  eperson_uuid UUID NULL,
  item_uuid UUID NULL,
  CONSTRAINT qaevent_pk PRIMARY KEY (qaevent_id),
  CONSTRAINT eperson_uuid_fkey FOREIGN KEY (eperson_uuid) REFERENCES eperson (uuid),
  CONSTRAINT item_uuid_fkey FOREIGN KEY (item_uuid) REFERENCES item (uuid)
);

CREATE INDEX item_uuid_idx ON qaevent_processed(item_uuid);
