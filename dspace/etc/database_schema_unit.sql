-------------------------------------------------------
-- Unit table
-------------------------------------------------------
CREATE SEQUENCE unit_seq;

CREATE TABLE Unit
(
  unit_id INTEGER PRIMARY KEY,
  name    VARCHAR(256) UNIQUE
);


-------------------------------------------------------
-- EPersonGroup2Unit table
-------------------------------------------------------
CREATE SEQUENCE epersongroup2unit_seq;

CREATE TABLE EPersonGroup2Unit
(
  id               INTEGER PRIMARY KEY,
  eperson_group_id INTEGER REFERENCES EPersonGroup(eperson_group_id),
  unit_id          INTEGER REFERENCES Unit(unit_id)
);

-- Index by group ID (used heavily by AuthorizeManager)
CREATE INDEX epersongroup2uniy_group_idx on EPersonGroup2Unit(eperson_group_id);

CREATE INDEX epg2unit_unit_fk_idx ON EPersonGroup2Unit(unit_id);


