-- Patch file to upgrade schema from database_schema.sql 1.22 (dspace-1_1, dspace-1_1_1) 
-- to revision 1.28 (current candidate for inclusion in dspace-1_2)

-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 

ALTER TABLE Bitstream ADD sequence_id INTEGER;
ALTER TABLE Item ADD owning_collection INTEGER;
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