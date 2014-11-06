--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- ===============================================================
-- WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
--
-- DO NOT MANUALLY RUN THIS DATABASE MIGRATION. IT WILL BE EXECUTED
-- AUTOMATICALLY (IF NEEDED) BY "FLYWAY" WHEN YOU STARTUP DSPACE.
-- http://flywaydb.org/
-- ===============================================================

CREATE SEQUENCE community2community_seq;
CREATE SEQUENCE communities2item_seq;

ALTER TABLE Bitstream ADD sequence_id INTEGER;

ALTER TABLE Item ADD owning_collection INTEGER;

-- The following changes the last_modified column from a TIMESTAMP to a
-- TIMESTAMP WITH TIME ZONE.  It copies over existing values of last_modified.
-- 
-- CAUTION:  This assumes that the values of the original 'last_modified'
-- column were in the *local time zone*.
--
-- If you find that the original values in 'last_modified' are in *UTC*,
-- (and this is not your local time zone), you will need to convert the
-- values of your last_modified column to your local time zone in order
-- for the following code to work.

ALTER TABLE Item ADD COLUMN last_modified2 TIMESTAMP WITH TIME ZONE;
UPDATE Item SET last_modified2 = last_modified;
ALTER TABLE Item DROP COLUMN last_modified;
ALTER TABLE Item RENAME last_modified2 TO last_modified;

ALTER TABLE Bundle ADD name VARCHAR(16);
ALTER TABLE Bundle ADD primary_bitstream_id INTEGER;
ALTER TABLE Bundle ADD CONSTRAINT primary_bitstream_id_fk FOREIGN KEY (primary_bitstream_id) REFERENCES Bitstream(bitstream_id);
CREATE TABLE Community2Community
(
  id             INTEGER PRIMARY KEY,
  parent_comm_id INTEGER REFERENCES Community(community_id),
  child_comm_id  INTEGER REFERENCES Community(community_id)
);

CREATE TABLE Communities2Item
(
   id                      INTEGER PRIMARY KEY,
   community_id            INTEGER REFERENCES Community(community_id),
   item_id                 INTEGER REFERENCES Item(item_id)
);

DROP VIEW CommunityItemsByAuthor;
CREATE VIEW CommunityItemsByAuthor as
SELECT Communities2Item.community_id, ItemsByAuthor.* 
FROM ItemsByAuthor, Communities2Item
WHERE ItemsByAuthor.item_id = Communities2Item.item_id
;

DROP VIEW CommunityItemsByTitle;
CREATE VIEW CommunityItemsByTitle as
SELECT Communities2Item.community_id, ItemsByTitle.* 
FROM ItemsByTitle, Communities2Item
WHERE ItemsByTitle.item_id = Communities2Item.item_id
;

DROP VIEW CommunityItemsByDate;
CREATE VIEW CommunityItemsByDate as
SELECT Communities2Item.community_id, ItemsByDate.* 
FROM ItemsByDate, Communities2Item
WHERE ItemsByDate.item_id = Communities2Item.item_id
;

DROP VIEW CommunityItemsByDateAccession;
CREATE VIEW CommunityItemsByDateAccession as
SELECT Communities2Item.community_id, ItemsByDateAccessioned.* 
FROM ItemsByDateAccessioned, Communities2Item
WHERE ItemsByDateAccessioned.item_id = Communities2Item.item_id
;
