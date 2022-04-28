--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

------------------------------------------------------
-- DS-3097 Handle of collections and communities are lost due to bug at V6.0_2015.03.07__DS-2701_Hibernate_migration.sql 
------------------------------------------------------

UPDATE handle SET resource_id = (SELECT community.uuid FROM community WHERE handle.resource_legacy_id = community.community_id AND handle.resource_type_id = 4) where handle.resource_type_id = 4;
UPDATE handle SET resource_id = (SELECT collection.uuid FROM collection WHERE handle.resource_legacy_id = collection.collection_id AND handle.resource_type_id = 3) where handle.resource_type_id = 3;
UPDATE handle SET resource_id = (SELECT item.uuid FROM item WHERE handle.resource_legacy_id = item.item_id AND handle.resource_type_id = 2) where handle.resource_type_id = 2;
 