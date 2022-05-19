-------------------------------------------------------
-- EtdUnit table
-------------------------------------------------------
CREATE SEQUENCE etdunit_seq;

CREATE TABLE EtdUnit
(
  etdunit_id INTEGER PRIMARY KEY,
  name    VARCHAR(256) UNIQUE,
);


-------------------------------------------------------
-- Collection2EtdUnit table
-------------------------------------------------------
CREATE SEQUENCE collection2etdunit_seq;

CREATE TABLE Collection2EtdUnit
(
  id             INTEGER PRIMARY KEY,
  collection_id  INTEGER REFERENCES Collection(collection_id),
  etdunit_id     INTEGER REFERENCES EtdUnit(etdunit_id),
);

-- Index by collection ID
CREATE INDEX collection2uniy_group_idx on Collection2EtdUnit(collection_id);

CREATE INDEX collection2etdunit_etdunit_fk_idx ON Collection2EtdUnit(etdunit_id);


