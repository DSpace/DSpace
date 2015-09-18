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
ALTER TABLE workflowitem ALTER COLUMN item_id rename to item_legacy_id;
ALTER TABLE workflowitem ADD COLUMN item_id UUID;
ALTER TABLE workflowitem ADD CONSTRAINT workflowitem_item_id_fk FOREIGN KEY (item_id) REFERENCES Item;
UPDATE workflowitem SET item_id = (SELECT item.uuid FROM item WHERE workflowitem.item_legacy_id = item.item_id);
ALTER TABLE workflowitem DROP COLUMN item_legacy_id;

-- Migrate task list item TODO: MOVE TO SEPARATE FILE IN POSTGRES/ORACLE
ALTER TABLE TasklistItem ALTER COLUMN eperson_id rename to eperson_legacy_id;
ALTER TABLE TasklistItem ADD COLUMN eperson_id UUID;
ALTER TABLE TasklistItem ADD CONSTRAINT TasklistItem_eperson_id_fk FOREIGN KEY (eperson_id) REFERENCES EPerson;
UPDATE TasklistItem SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE TasklistItem.eperson_legacy_id = eperson.eperson_id);
ALTER TABLE TasklistItem DROP COLUMN eperson_legacy_id;




ALTER TABLE workflowitem ALTER COLUMN collection_id rename to collection_legacy_id;
ALTER TABLE workflowitem ADD COLUMN collection_id UUID;
ALTER TABLE workflowitem ADD CONSTRAINT workflowitem_collection_id_fk FOREIGN KEY (collection_id) REFERENCES collection;
UPDATE workflowitem SET collection_id = (SELECT collection.uuid FROM collection WHERE workflowitem.collection_legacy_id = collection.collection_id);
ALTER TABLE workflowitem DROP COLUMN collection_legacy_id;

ALTER TABLE workflowitem ALTER COLUMN owner rename to owner_legacy_id;
ALTER TABLE workflowitem ADD COLUMN owner UUID;
ALTER TABLE workflowitem ADD CONSTRAINT workflowitem_owner_id_fk FOREIGN KEY (owner) REFERENCES eperson;
UPDATE workflowitem SET owner = (SELECT eperson.uuid FROM eperson WHERE workflowitem.owner_legacy_id = eperson.eperson_id);
ALTER TABLE workflowitem DROP COLUMN owner_legacy_id;
