--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

------------------------------------------------------
-- DS-2701 Service based API / Hibernate integration
------------------------------------------------------
-- Alter workflow item
ALTER TABLE workflowitem RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE workflowitem ADD item_id RAW(16) REFERENCES Item(uuid);
UPDATE workflowitem SET item_id = (SELECT item.uuid FROM item WHERE workflowitem.item_legacy_id = item.item_id);
ALTER TABLE workflowitem DROP COLUMN item_legacy_id;

-- Migrate task list item
ALTER TABLE TasklistItem RENAME COLUMN eperson_id to eperson_legacy_id;
ALTER TABLE TasklistItem ADD eperson_id RAW(16) REFERENCES EPerson(uuid);
UPDATE TasklistItem SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE TasklistItem.eperson_legacy_id = eperson.eperson_id);
ALTER TABLE TasklistItem DROP COLUMN eperson_legacy_id;

-- Migrate task workflow item
ALTER TABLE workflowitem RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE workflowitem ADD collection_id RAW(16) REFERENCES Collection(uuid);
UPDATE workflowitem SET collection_id = (SELECT collection.uuid FROM collection WHERE workflowitem.collection_legacy_id = collection.collection_id);
ALTER TABLE workflowitem DROP COLUMN collection_legacy_id;
ALTER TABLE workflowitem RENAME COLUMN owner to owner_legacy_id;
ALTER TABLE workflowitem ADD owner RAW(16) REFERENCES EPerson (uuid);
UPDATE workflowitem SET owner = (SELECT eperson.uuid FROM eperson WHERE workflowitem.owner_legacy_id = eperson.eperson_id);
ALTER TABLE workflowitem DROP COLUMN owner_legacy_id;
UPDATE workflowitem SET state = -1 WHERE state IS NULL;
UPDATE workflowitem SET multiple_titles = '0' WHERE multiple_titles IS NULL;
UPDATE workflowitem SET published_before = '0' WHERE published_before IS NULL;
UPDATE workflowitem SET multiple_files = '0' WHERE multiple_files IS NULL;

