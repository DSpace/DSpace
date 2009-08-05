CREATE SEQUENCE unit_seq;

-------------------------------------------------------
-- Unit table
-------------------------------------------------------
CREATE TABLE Unit
(
  unit_id INTEGER PRIMARY KEY,
  name    VARCHAR(256) UNIQUE
);
