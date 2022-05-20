-- From "V1.7.2_3__drum_database_schema_unit.sql" and moved here to ensure that
-- the "Unit" table is created, before it is modified in the next migration.

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
  unit_id          INTEGER REFERENCES Unit(unit_id),
);

-- Index by group ID (used heavily by AuthorizeManager)
CREATE INDEX epersongroup2uniy_group_idx on EPersonGroup2Unit(eperson_group_id);

CREATE INDEX epg2unit_unit_fk_idx ON EPersonGroup2Unit(unit_id);

-- End of changes from  "V1.7.2_3__drum_database_schema_unit.sql"

ALTER TABLE unit ADD COLUMN faculty_only BOOL;

UPDATE unit SET faculty_only=true;

