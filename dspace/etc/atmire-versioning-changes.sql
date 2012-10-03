-- CREATE TABLE versionhistory
-- (
--   versionhistory_id integer NOT NULL PRIMARY KEY
-- );
--
-- CREATE TABLE versionitem
-- (
--   versionitem_id integer NOT NULL PRIMARY KEY,
--   item_id INTEGER,
--   version_number integer,
--   eperson_id INTEGER,
--   version_date TIMESTAMP,
--   version_summary VARCHAR(255),
--   versionhistory_id INTEGER
-- );
--
-- ALTER TABLE ONLY versionitem
--     ADD CONSTRAINT item_id_fkey FOREIGN KEY (item_id) REFERENCES item(item_id);
--
-- ALTER TABLE ONLY versionitem
--     ADD CONSTRAINT eperson_id_fkey FOREIGN KEY (eperson_id) REFERENCES eperson(eperson_id);
--
-- ALTER TABLE ONLY versionitem
--     ADD CONSTRAINT versionhistory_id_fkey FOREIGN KEY (versionhistory_id) REFERENCES versionhistory(versionhistory_id);
--
--
-- CREATE SEQUENCE versionitem_seq;
-- CREATE SEQUENCE versionhistory_seq;


--DROP TABLE versionitem;
--DROP TABLE versionhistory;

--DROP SEQUENCE versionitem_seq;
--DROP SEQUENCE versionhistory_seq;
CREATE TABLE versionhistory
(
  versionhistory_id integer NOT NULL PRIMARY KEY
);

CREATE TABLE versionitem
(
  versionitem_id integer NOT NULL PRIMARY KEY,
  item_id INTEGER REFERENCES Item(item_id),
  version_number integer,
  eperson_id INTEGER REFERENCES EPerson(eperson_id),
  version_date TIMESTAMP,
  version_summary VARCHAR(255),
  versionhistory_id INTEGER REFERENCES VersionHistory(versionhistory_id)
);

CREATE SEQUENCE versionitem_seq;
CREATE SEQUENCE versionhistory_seq;
