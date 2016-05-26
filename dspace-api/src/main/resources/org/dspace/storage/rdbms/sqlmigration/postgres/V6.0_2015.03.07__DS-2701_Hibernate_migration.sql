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
DROP VIEW community2item;

CREATE TABLE dspaceobject
(
    uuid            uuid NOT NULL  PRIMARY KEY
);

CREATE TABLE site
(
    uuid            uuid NOT NULL PRIMARY KEY REFERENCES dspaceobject(uuid)

);

ALTER TABLE eperson ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM eperson;
ALTER TABLE eperson ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE eperson ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE eperson ADD CONSTRAINT eperson_id_unique UNIQUE(uuid);
ALTER TABLE eperson ADD PRIMARY KEY (uuid);
ALTER TABLE eperson ALTER COLUMN eperson_id DROP NOT NULL;
CREATE INDEX eperson_id_idx on eperson(eperson_id);
UPDATE eperson SET require_certificate = false WHERE require_certificate IS NULL;
UPDATE eperson SET self_registered = false WHERE self_registered IS NULL;



UPDATE metadatavalue SET text_value='Administrator'
  WHERE resource_type_id=6 AND resource_id=1;
UPDATE metadatavalue SET text_value='Anonymous'
  WHERE resource_type_id=6 AND resource_id=0;

ALTER TABLE epersongroup ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM epersongroup;
ALTER TABLE epersongroup ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE epersongroup ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE epersongroup ADD CONSTRAINT epersongroup_id_unique UNIQUE(uuid);
ALTER TABLE epersongroup ADD PRIMARY KEY (uuid);
ALTER TABLE epersongroup ALTER COLUMN eperson_group_id DROP NOT NULL;
CREATE INDEX eperson_group_id_idx on epersongroup(eperson_group_id);

ALTER TABLE item ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM item;
ALTER TABLE item ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE item ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE item ADD CONSTRAINT item_id_unique UNIQUE(uuid);
ALTER TABLE item ADD PRIMARY KEY (uuid);
ALTER TABLE item ALTER COLUMN item_id DROP NOT NULL;
CREATE INDEX item_id_idx on item(item_id);

ALTER TABLE community ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM community;
ALTER TABLE community ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE community ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE community ADD CONSTRAINT community_id_unique UNIQUE(uuid);
ALTER TABLE community ADD PRIMARY KEY (uuid);
ALTER TABLE community ALTER COLUMN community_id DROP NOT NULL;
CREATE INDEX community_id_idx on community(community_id);


ALTER TABLE collection ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM collection;
ALTER TABLE collection ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE collection ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE collection ADD CONSTRAINT collection_id_unique UNIQUE(uuid);
ALTER TABLE collection ADD PRIMARY KEY (uuid);
ALTER TABLE collection ALTER COLUMN collection_id DROP NOT NULL;
CREATE INDEX collection_id_idx on collection(collection_id);

ALTER TABLE bundle ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM bundle;
ALTER TABLE bundle ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE bundle ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE bundle ADD CONSTRAINT bundle_id_unique UNIQUE(uuid);
ALTER TABLE bundle ADD PRIMARY KEY (uuid);
ALTER TABLE bundle ALTER COLUMN bundle_id DROP NOT NULL;
CREATE INDEX bundle_id_idx on bundle(bundle_id);

ALTER TABLE bitstream ADD COLUMN uuid UUID DEFAULT gen_random_uuid() UNIQUE;
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM bitstream;
ALTER TABLE bitstream ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE bitstream ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE bitstream ADD CONSTRAINT bitstream_id_unique UNIQUE(uuid);
ALTER TABLE bitstream ADD PRIMARY KEY (uuid);
ALTER TABLE bitstream ALTER COLUMN bitstream_id DROP NOT NULL;
CREATE INDEX bitstream_id_idx on bitstream(bitstream_id);
UPDATE bitstream SET sequence_id = -1 WHERE sequence_id IS NULL;
UPDATE bitstream SET size_bytes = -1 WHERE size_bytes IS NULL;
UPDATE bitstream SET deleted = FALSE WHERE deleted IS NULL;
UPDATE bitstream SET store_number = -1 WHERE store_number IS NULL;

-- Migrate EPersonGroup2EPerson table
ALTER TABLE EPersonGroup2EPerson RENAME COLUMN eperson_group_id to eperson_group_legacy_id;
ALTER TABLE EPersonGroup2EPerson RENAME COLUMN eperson_id to eperson_legacy_id;
ALTER TABLE EPersonGroup2EPerson ADD COLUMN eperson_group_id UUID REFERENCES EpersonGroup(uuid);
ALTER TABLE EPersonGroup2EPerson ADD COLUMN eperson_id UUID REFERENCES Eperson(uuid);
UPDATE EPersonGroup2EPerson SET eperson_group_id = EPersonGroup.uuid FROM EpersonGroup WHERE EPersonGroup2EPerson.eperson_group_legacy_id = EPersonGroup.eperson_group_id;
UPDATE EPersonGroup2EPerson SET eperson_id = eperson.uuid FROM eperson WHERE EPersonGroup2EPerson.eperson_legacy_id = eperson.eperson_id;
ALTER TABLE EPersonGroup2EPerson ALTER COLUMN eperson_group_id SET NOT NULL;
ALTER TABLE EPersonGroup2EPerson ALTER COLUMN eperson_id SET NOT NULL;
ALTER TABLE EPersonGroup2EPerson DROP COLUMN eperson_group_legacy_id;
ALTER TABLE EPersonGroup2EPerson DROP COLUMN eperson_legacy_id;
ALTER TABLE epersongroup2eperson DROP COLUMN id;
ALTER TABLE EPersonGroup2EPerson add primary key (eperson_group_id,eperson_id);
CREATE INDEX EpersonGroup2Eperson_group on EpersonGroup2Eperson(eperson_group_id);
CREATE INDEX EpersonGroup2Eperson_person on EpersonGroup2Eperson(eperson_id);

-- Migrate GROUP2GROUP table
ALTER TABLE Group2Group RENAME COLUMN parent_id to parent_legacy_id;
ALTER TABLE Group2Group RENAME COLUMN child_id to child_legacy_id;
ALTER TABLE Group2Group ADD COLUMN parent_id UUID REFERENCES EpersonGroup(uuid);
ALTER TABLE Group2Group ADD COLUMN child_id UUID REFERENCES EpersonGroup(uuid);
UPDATE Group2Group SET parent_id = EPersonGroup.uuid FROM EpersonGroup WHERE Group2Group.parent_legacy_id = EPersonGroup.eperson_group_id;
UPDATE Group2Group SET child_id = EpersonGroup.uuid FROM EpersonGroup WHERE Group2Group.child_legacy_id = EpersonGroup.eperson_group_id;
ALTER TABLE Group2Group ALTER COLUMN parent_id SET NOT NULL;
ALTER TABLE Group2Group ALTER COLUMN child_id SET NOT NULL;
ALTER TABLE Group2Group DROP COLUMN parent_legacy_id;
ALTER TABLE Group2Group DROP COLUMN child_legacy_id;
ALTER TABLE Group2Group DROP COLUMN id;
ALTER TABLE Group2Group add primary key (parent_id,child_id);
CREATE INDEX Group2Group_parent on Group2Group(parent_id);
CREATE INDEX Group2Group_child on Group2Group(child_id);

-- Migrate collection2item
ALTER TABLE Collection2Item RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE Collection2Item RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE Collection2Item ADD COLUMN collection_id UUID REFERENCES Collection(uuid);
ALTER TABLE Collection2Item ADD COLUMN item_id UUID REFERENCES Item(uuid);
UPDATE Collection2Item SET collection_id = Collection.uuid FROM Collection WHERE Collection2Item.collection_legacy_id = Collection.collection_id;
UPDATE Collection2Item SET item_id = Item.uuid FROM Item WHERE Collection2Item.item_legacy_id = Item.item_id;
ALTER TABLE Collection2Item ALTER COLUMN collection_id SET NOT NULL;
ALTER TABLE Collection2Item ALTER COLUMN item_id SET NOT NULL;
ALTER TABLE Collection2Item DROP COLUMN collection_legacy_id;
ALTER TABLE Collection2Item DROP COLUMN item_legacy_id;
ALTER TABLE Collection2Item DROP COLUMN id;
CREATE INDEX Collecion2Item_collection on Collection2Item(collection_id);
CREATE INDEX Collecion2Item_item on Collection2Item(item_id);

-- Magic query that will delete all duplicate collection item_id references from the database (if we don't do this the primary key creation will fail)
DELETE FROM collection2item a USING (
      SELECT max(ctid) as ctid, collection_id,item_id
        FROM collection2item 
        GROUP BY collection_id,item_id HAVING COUNT(*) > 1
      ) b
WHERE a.collection_id = b.collection_id
AND a.item_id = b.item_id
AND a.ctid <> b.ctid;

ALTER TABLE Collection2Item add primary key (collection_id,item_id);

-- Migrate Community2Community
ALTER TABLE Community2Community RENAME COLUMN parent_comm_id to parent_legacy_id;
ALTER TABLE Community2Community RENAME COLUMN child_comm_id to child_legacy_id;
ALTER TABLE Community2Community ADD COLUMN parent_comm_id UUID REFERENCES Community(uuid);
ALTER TABLE Community2Community ADD COLUMN child_comm_id UUID REFERENCES Community(uuid);
UPDATE Community2Community SET parent_comm_id = Community.uuid FROM Community WHERE Community2Community.parent_legacy_id = Community.community_id;
UPDATE Community2Community SET child_comm_id = Community.uuid FROM Community WHERE Community2Community.child_legacy_id = Community.community_id;
ALTER TABLE Community2Community ALTER COLUMN parent_comm_id SET NOT NULL;
ALTER TABLE Community2Community ALTER COLUMN child_comm_id SET NOT NULL;
ALTER TABLE Community2Community DROP COLUMN parent_legacy_id;
ALTER TABLE Community2Community DROP COLUMN child_legacy_id;
ALTER TABLE Community2Community DROP COLUMN id;
ALTER TABLE Community2Community add primary key (parent_comm_id,child_comm_id);
CREATE INDEX Community2Community_parent on Community2Community(parent_comm_id);
CREATE INDEX Community2Community_child on Community2Community(child_comm_id); 

-- Migrate community2collection
ALTER TABLE community2collection RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE community2collection RENAME COLUMN community_id to community_legacy_id;
ALTER TABLE community2collection ADD COLUMN collection_id UUID REFERENCES Collection(uuid);
ALTER TABLE community2collection ADD COLUMN community_id UUID REFERENCES Community(uuid);
UPDATE community2collection SET collection_id = Collection.uuid FROM Collection WHERE community2collection.collection_legacy_id = Collection.collection_id;
UPDATE community2collection SET community_id = Community.uuid FROM Community WHERE community2collection.community_legacy_id = Community.community_id;
ALTER TABLE community2collection ALTER COLUMN collection_id SET NOT NULL;
ALTER TABLE community2collection ALTER COLUMN community_id SET NOT NULL;
ALTER TABLE community2collection DROP COLUMN collection_legacy_id;
ALTER TABLE community2collection DROP COLUMN community_legacy_id;
ALTER TABLE community2collection DROP COLUMN id;
ALTER TABLE community2collection add primary key (collection_id,community_id);
CREATE INDEX community2collection_collection on community2collection(collection_id);
CREATE INDEX community2collection_community on community2collection(community_id);

-- Migrate Group2GroupCache table
ALTER TABLE Group2GroupCache RENAME COLUMN parent_id to parent_legacy_id;
ALTER TABLE Group2GroupCache RENAME COLUMN child_id to child_legacy_id;
ALTER TABLE Group2GroupCache ADD COLUMN parent_id UUID REFERENCES EpersonGroup(uuid);
ALTER TABLE Group2GroupCache ADD COLUMN child_id UUID REFERENCES EpersonGroup(uuid);
UPDATE Group2GroupCache SET parent_id = EPersonGroup.uuid FROM EpersonGroup WHERE Group2GroupCache.parent_legacy_id = EPersonGroup.eperson_group_id;
UPDATE Group2GroupCache SET child_id = EpersonGroup.uuid FROM EpersonGroup WHERE Group2GroupCache.child_legacy_id = EpersonGroup.eperson_group_id;
ALTER TABLE Group2GroupCache ALTER COLUMN parent_id SET NOT NULL;
ALTER TABLE Group2GroupCache ALTER COLUMN child_id SET NOT NULL;
ALTER TABLE Group2GroupCache DROP COLUMN parent_legacy_id;
ALTER TABLE Group2GroupCache DROP COLUMN child_legacy_id;
ALTER TABLE Group2GroupCache DROP COLUMN id;
ALTER TABLE Group2GroupCache add primary key (parent_id,child_id);
CREATE INDEX Group2GroupCache_parent on Group2GroupCache(parent_id);
CREATE INDEX Group2GroupCache_child on Group2GroupCache(child_id);

-- Migrate Item2Bundle
ALTER TABLE item2bundle RENAME COLUMN bundle_id to bundle_legacy_id;
ALTER TABLE item2bundle RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE item2bundle ADD COLUMN bundle_id UUID REFERENCES Bundle(uuid);
ALTER TABLE item2bundle ADD COLUMN item_id UUID REFERENCES Item(uuid);
UPDATE item2bundle SET bundle_id = Bundle.uuid FROM Bundle WHERE item2bundle.bundle_legacy_id = Bundle.bundle_id;
UPDATE item2bundle SET item_id = Item.uuid FROM Item WHERE item2bundle.item_legacy_id = Item.item_id;
ALTER TABLE item2bundle ALTER COLUMN bundle_id SET NOT NULL;
ALTER TABLE item2bundle ALTER COLUMN item_id SET NOT NULL;
ALTER TABLE item2bundle DROP COLUMN bundle_legacy_id;
ALTER TABLE item2bundle DROP COLUMN item_legacy_id;
ALTER TABLE item2bundle DROP COLUMN id;
ALTER TABLE item2bundle add primary key (bundle_id,item_id);
CREATE INDEX item2bundle_bundle on item2bundle(bundle_id);
CREATE INDEX item2bundle_item on item2bundle(item_id);

--Migrate Bundle2Bitsteam
ALTER TABLE bundle2bitstream RENAME COLUMN bundle_id to bundle_legacy_id;
ALTER TABLE bundle2bitstream RENAME COLUMN bitstream_id to bitstream_legacy_id;
ALTER TABLE bundle2bitstream ADD COLUMN bundle_id UUID REFERENCES Bundle(uuid);
ALTER TABLE bundle2bitstream ADD COLUMN bitstream_id UUID REFERENCES Bitstream(uuid);
UPDATE bundle2bitstream SET bundle_id = (SELECT bundle.uuid FROM bundle WHERE bundle2bitstream.bundle_legacy_id = bundle.bundle_id);
UPDATE bundle2bitstream SET bitstream_id = (SELECT bitstream.uuid FROM bitstream WHERE bundle2bitstream.bitstream_legacy_id = bitstream.bitstream_id);
ALTER TABLE bundle2bitstream RENAME COLUMN bitstream_order to bitstream_order_legacy;
ALTER TABLE bundle2bitstream ADD COLUMN bitstream_order INTEGER;
UPDATE bundle2bitstream SET bitstream_order = b2b_ranked.order FROM (SELECT bundle_id, bitstream_id,(rank() OVER (PARTITION BY bundle_id ORDER BY bitstream_order_legacy, bitstream_id) -1) as order FROM bundle2bitstream) b2b_ranked WHERE bundle2bitstream.bundle_id = b2b_ranked.bundle_id AND bundle2bitstream.bitstream_id = b2b_ranked.bitstream_id;
ALTER TABLE bundle2bitstream ALTER COLUMN bundle_id SET NOT NULL;
ALTER TABLE bundle2bitstream ALTER COLUMN bitstream_id SET NOT NULL;
ALTER TABLE bundle2bitstream DROP COLUMN bundle_legacy_id;
ALTER TABLE bundle2bitstream DROP COLUMN bitstream_legacy_id;
ALTER TABLE bundle2bitstream DROP COLUMN id;
ALTER TABLE bundle2bitstream add primary key (bitstream_id,bundle_id,bitstream_order);
CREATE INDEX bundle2bitstream_bundle on bundle2bitstream(bundle_id);
CREATE INDEX bundle2bitstream_bitstream on bundle2bitstream(bitstream_id);

-- Migrate item
ALTER TABLE item RENAME COLUMN submitter_id to submitter_id_legacy_id;
ALTER TABLE item ADD COLUMN submitter_id UUID REFERENCES EPerson(uuid);
UPDATE item SET submitter_id = eperson.uuid FROM eperson WHERE item.submitter_id_legacy_id = eperson.eperson_id;
ALTER TABLE item DROP COLUMN submitter_id_legacy_id;
CREATE INDEX item_submitter on item(submitter_id);

ALTER TABLE item RENAME COLUMN owning_collection to owning_collection_legacy;
ALTER TABLE item ADD COLUMN owning_collection UUID REFERENCES Collection(uuid);
UPDATE item SET owning_collection = Collection.uuid FROM Collection WHERE item.owning_collection_legacy = collection.collection_id;
ALTER TABLE item DROP COLUMN owning_collection_legacy;
CREATE INDEX item_collection on item(owning_collection);
UPDATE item SET in_archive = FALSE WHERE in_archive IS NULL;
UPDATE item SET discoverable = FALSE WHERE discoverable IS NULL;
UPDATE item SET withdrawn = FALSE WHERE withdrawn IS NULL;

-- Migrate bundle
ALTER TABLE bundle RENAME COLUMN primary_bitstream_id to primary_bitstream_legacy_id;
ALTER TABLE bundle ADD COLUMN primary_bitstream_id UUID REFERENCES Bitstream(uuid);
UPDATE bundle SET primary_bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE bundle.primary_bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE bundle DROP COLUMN primary_bitstream_legacy_id;
CREATE INDEX bundle_primary on bundle(primary_bitstream_id);

-- Migrate community references
ALTER TABLE Community RENAME COLUMN admin to admin_legacy;
ALTER TABLE Community ADD COLUMN admin UUID REFERENCES EPersonGroup(uuid);
UPDATE Community SET admin = EPersonGroup.uuid FROM EPersonGroup WHERE Community.admin_legacy = EPersonGroup.eperson_group_id;
ALTER TABLE Community DROP COLUMN admin_legacy;
CREATE INDEX Community_admin on Community(admin);

ALTER TABLE Community RENAME COLUMN logo_bitstream_id to logo_bitstream_legacy_id;
ALTER TABLE Community ADD COLUMN logo_bitstream_id UUID REFERENCES Bitstream(uuid);
UPDATE Community SET logo_bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE Community.logo_bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE Community DROP COLUMN logo_bitstream_legacy_id;
CREATE INDEX Community_bitstream on Community(logo_bitstream_id);


--Migrate Collection references
ALTER TABLE Collection RENAME COLUMN workflow_step_1 to workflow_step_1_legacy;
ALTER TABLE Collection RENAME COLUMN workflow_step_2 to workflow_step_2_legacy;
ALTER TABLE Collection RENAME COLUMN workflow_step_3 to workflow_step_3_legacy;
ALTER TABLE Collection RENAME COLUMN submitter to submitter_legacy;
ALTER TABLE Collection RENAME COLUMN template_item_id to template_item_legacy_id;
ALTER TABLE Collection RENAME COLUMN logo_bitstream_id to logo_bitstream_legacy_id;
ALTER TABLE Collection RENAME COLUMN admin to admin_legacy;
ALTER TABLE Collection ADD COLUMN workflow_step_1 UUID REFERENCES EPersonGroup(uuid);
ALTER TABLE Collection ADD COLUMN workflow_step_2 UUID REFERENCES EPersonGroup(uuid);
ALTER TABLE Collection ADD COLUMN workflow_step_3 UUID REFERENCES EPersonGroup(uuid);
ALTER TABLE Collection ADD COLUMN submitter UUID REFERENCES EPersonGroup(uuid);
ALTER TABLE Collection ADD COLUMN template_item_id UUID;
ALTER TABLE Collection ADD COLUMN logo_bitstream_id UUID;
ALTER TABLE Collection ADD COLUMN admin UUID REFERENCES EPersonGroup(uuid);
UPDATE Collection SET workflow_step_1 = EPersonGroup.uuid FROM EPersonGroup WHERE Collection.workflow_step_1_legacy = EPersonGroup.eperson_group_id;
UPDATE Collection SET workflow_step_2 = EPersonGroup.uuid FROM EPersonGroup WHERE Collection.workflow_step_2_legacy = EPersonGroup.eperson_group_id;
UPDATE Collection SET workflow_step_3 = EPersonGroup.uuid FROM EPersonGroup WHERE Collection.workflow_step_3_legacy = EPersonGroup.eperson_group_id;
UPDATE Collection SET submitter = EPersonGroup.uuid FROM EPersonGroup WHERE Collection.submitter_legacy = EPersonGroup.eperson_group_id;
UPDATE Collection SET template_item_id = (SELECT Item.uuid FROM Item WHERE Collection.template_item_legacy_id = Item.item_id);
UPDATE Collection SET logo_bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE Collection.logo_bitstream_legacy_id = Bitstream.bitstream_id);
UPDATE Collection SET admin = EPersonGroup.uuid FROM EPersonGroup WHERE Collection.admin_legacy = EPersonGroup.eperson_group_id;
ALTER TABLE Collection DROP COLUMN workflow_step_1_legacy;
ALTER TABLE Collection DROP COLUMN workflow_step_2_legacy;
ALTER TABLE Collection DROP COLUMN workflow_step_3_legacy;
ALTER TABLE Collection DROP COLUMN submitter_legacy;
ALTER TABLE Collection DROP COLUMN template_item_legacy_id;
ALTER TABLE Collection DROP COLUMN logo_bitstream_legacy_id;
ALTER TABLE Collection DROP COLUMN admin_legacy;

CREATE INDEX Collection_workflow1 on Collection(workflow_step_1);
CREATE INDEX Collection_workflow2 on Collection(workflow_step_2);
CREATE INDEX Collection_workflow3 on Collection(workflow_step_3);
CREATE INDEX Collection_submitter on Collection(submitter);
CREATE INDEX Collection_template on Collection(template_item_id);
CREATE INDEX Collection_bitstream on Collection(logo_bitstream_id);

-- Migrate resource policy references
ALTER TABLE ResourcePolicy RENAME COLUMN eperson_id to eperson_id_legacy_id;
ALTER TABLE ResourcePolicy ADD COLUMN eperson_id UUID REFERENCES EPerson(uuid);
UPDATE ResourcePolicy SET eperson_id = eperson.uuid FROM eperson WHERE ResourcePolicy.eperson_id_legacy_id = eperson.eperson_id;
ALTER TABLE ResourcePolicy DROP COLUMN eperson_id_legacy_id;
ALTER TABLE ResourcePolicy RENAME COLUMN epersongroup_id to epersongroup_id_legacy_id;
ALTER TABLE ResourcePolicy ADD COLUMN epersongroup_id UUID REFERENCES EPersonGroup(uuid);
UPDATE ResourcePolicy SET epersongroup_id = epersongroup.uuid FROM epersongroup WHERE ResourcePolicy.epersongroup_id_legacy_id = epersongroup.eperson_group_id;
ALTER TABLE ResourcePolicy DROP COLUMN epersongroup_id_legacy_id;

ALTER TABLE ResourcePolicy ADD COLUMN dspace_object UUID REFERENCES dspaceobject(uuid);
UPDATE ResourcePolicy SET dspace_object = (SELECT eperson.uuid FROM eperson WHERE ResourcePolicy.resource_id = eperson.eperson_id AND ResourcePolicy.resource_type_id = 7) WHERE ResourcePolicy.resource_type_id = 7;
UPDATE ResourcePolicy SET dspace_object = (SELECT epersongroup.uuid FROM epersongroup WHERE ResourcePolicy.resource_id = epersongroup.eperson_group_id AND ResourcePolicy.resource_type_id = 6)  WHERE ResourcePolicy.resource_type_id = 6;
UPDATE ResourcePolicy SET dspace_object = (SELECT community.uuid FROM community WHERE ResourcePolicy.resource_id = community.community_id AND ResourcePolicy.resource_type_id = 4)  WHERE ResourcePolicy.resource_type_id = 4;
UPDATE ResourcePolicy SET dspace_object = (SELECT collection.uuid FROM collection WHERE ResourcePolicy.resource_id = collection.collection_id AND ResourcePolicy.resource_type_id = 3)  WHERE ResourcePolicy.resource_type_id = 3;
UPDATE ResourcePolicy SET dspace_object = (SELECT item.uuid FROM item WHERE ResourcePolicy.resource_id = item.item_id AND ResourcePolicy.resource_type_id = 2)  WHERE ResourcePolicy.resource_type_id = 2;
UPDATE ResourcePolicy SET dspace_object = (SELECT bundle.uuid FROM bundle WHERE ResourcePolicy.resource_id = bundle.bundle_id AND ResourcePolicy.resource_type_id = 1)  WHERE ResourcePolicy.resource_type_id = 1;
UPDATE ResourcePolicy SET dspace_object = (SELECT bitstream.uuid FROM bitstream WHERE ResourcePolicy.resource_id = bitstream.bitstream_id AND ResourcePolicy.resource_type_id = 0)  WHERE ResourcePolicy.resource_type_id = 0;
UPDATE resourcepolicy SET resource_type_id = -1 WHERE resource_type_id IS NULL;
UPDATE resourcepolicy SET action_id = -1 WHERE action_id IS NULL;

CREATE INDEX resourcepolicy_person on resourcepolicy(eperson_id);
CREATE INDEX resourcepolicy_group on resourcepolicy(epersongroup_id);
CREATE INDEX resourcepolicy_object on resourcepolicy(dspace_object);

-- Migrate Subscription
ALTER TABLE Subscription RENAME COLUMN eperson_id to eperson_legacy_id;
ALTER TABLE Subscription ADD COLUMN eperson_id UUID REFERENCES EPerson(uuid);
UPDATE Subscription SET eperson_id = eperson.uuid FROM eperson WHERE Subscription.eperson_legacy_id = eperson.eperson_id;
ALTER TABLE Subscription DROP COLUMN eperson_legacy_id;
ALTER TABLE Subscription RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE Subscription ADD COLUMN collection_id UUID REFERENCES Collection(uuid);
UPDATE Subscription SET collection_id = (SELECT collection.uuid FROM collection WHERE Subscription.collection_legacy_id = collection.collection_id);
ALTER TABLE Subscription DROP COLUMN collection_legacy_id;
CREATE INDEX Subscription_person on Subscription(eperson_id);
CREATE INDEX Subscription_collection on Subscription(collection_id);

-- Migrate versionitem
ALTER TABLE versionitem RENAME COLUMN eperson_id to eperson_legacy_id;
ALTER TABLE versionitem ADD COLUMN eperson_id UUID REFERENCES EPerson(uuid);
UPDATE versionitem SET eperson_id = eperson.uuid FROM eperson WHERE versionitem.eperson_legacy_id = eperson.eperson_id;
ALTER TABLE versionitem DROP COLUMN eperson_legacy_id;
CREATE INDEX versionitem_person on versionitem(eperson_id);

ALTER TABLE versionitem RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE versionitem ADD COLUMN item_id UUID REFERENCES Item(uuid);
UPDATE versionitem SET item_id = (SELECT item.uuid FROM item WHERE versionitem.item_legacy_id = item.item_id);
ALTER TABLE versionitem DROP COLUMN item_legacy_id;
UPDATE versionitem SET version_number = -1 WHERE version_number IS NULL;
CREATE INDEX versionitem_item on versionitem(item_id);

-- Migrate handle table
ALTER TABLE handle RENAME COLUMN resource_id to resource_legacy_id;
ALTER TABLE handle ADD COLUMN resource_id UUID REFERENCES dspaceobject(uuid);
UPDATE handle SET resource_id = community.uuid FROM community WHERE handle.resource_legacy_id = community.community_id AND handle.resource_type_id = 4;
UPDATE handle SET resource_id = collection.uuid FROM collection WHERE handle.resource_legacy_id = collection.collection_id AND handle.resource_type_id = 3;
UPDATE handle SET resource_id = item.uuid FROM item WHERE handle.resource_legacy_id = item.item_id AND handle.resource_type_id = 2;
UPDATE handle SET resource_type_id = -1 WHERE resource_type_id IS NULL;
CREATE INDEX handle_object on handle(resource_id);

-- Migrate metadata value table
DROP VIEW dcvalue;

ALTER TABLE metadatavalue ADD COLUMN dspace_object_id UUID REFERENCES dspaceobject(uuid);
UPDATE metadatavalue SET dspace_object_id = (SELECT eperson.uuid FROM eperson WHERE metadatavalue.resource_id = eperson.eperson_id AND metadatavalue.resource_type_id = 7) WHERE metadatavalue.resource_type_id= 7;
UPDATE metadatavalue SET dspace_object_id = (SELECT epersongroup.uuid FROM epersongroup WHERE metadatavalue.resource_id = epersongroup.eperson_group_id AND metadatavalue.resource_type_id = 6) WHERE metadatavalue.resource_type_id= 6;
UPDATE metadatavalue SET dspace_object_id = (SELECT community.uuid FROM community WHERE metadatavalue.resource_id = community.community_id AND metadatavalue.resource_type_id = 4) WHERE metadatavalue.resource_type_id= 4;
UPDATE metadatavalue SET dspace_object_id = (SELECT collection.uuid FROM collection WHERE metadatavalue.resource_id = collection.collection_id AND metadatavalue.resource_type_id = 3) WHERE metadatavalue.resource_type_id= 3;
UPDATE metadatavalue SET dspace_object_id = (SELECT item.uuid FROM item WHERE metadatavalue.resource_id = item.item_id AND metadatavalue.resource_type_id = 2) WHERE metadatavalue.resource_type_id= 2;
UPDATE metadatavalue SET dspace_object_id = (SELECT bundle.uuid FROM bundle WHERE metadatavalue.resource_id = bundle.bundle_id AND metadatavalue.resource_type_id = 1) WHERE metadatavalue.resource_type_id= 1;
UPDATE metadatavalue SET dspace_object_id = (SELECT bitstream.uuid FROM bitstream WHERE metadatavalue.resource_id = bitstream.bitstream_id AND metadatavalue.resource_type_id = 0) WHERE metadatavalue.resource_type_id= 0;
DROP INDEX metadatavalue_item_idx;
DROP INDEX metadatavalue_item_idx2;
ALTER TABLE metadatavalue DROP COLUMN IF EXISTS resource_id;
ALTER TABLE metadatavalue DROP COLUMN resource_type_id;
UPDATE MetadataValue SET confidence = -1 WHERE confidence IS NULL;
UPDATE metadatavalue SET place = -1 WHERE place IS NULL;
CREATE INDEX metadatavalue_object on metadatavalue(dspace_object_id);
CREATE INDEX metadatavalue_field on metadatavalue(metadata_field_id);
CREATE INDEX metadatavalue_field_object on metadatavalue(metadata_field_id, dspace_object_id);

-- Alter harvested item
ALTER TABLE harvested_item RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE harvested_item ADD COLUMN item_id UUID REFERENCES item(uuid);
UPDATE harvested_item SET item_id = (SELECT item.uuid FROM item WHERE harvested_item.item_legacy_id = item.item_id);
ALTER TABLE harvested_item DROP COLUMN item_legacy_id;
CREATE INDEX harvested_item_item on harvested_item(item_id);

-- Alter harvested collection
ALTER TABLE harvested_collection RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE harvested_collection ADD COLUMN collection_id UUID REFERENCES Collection(uuid);
UPDATE harvested_collection SET collection_id = (SELECT collection.uuid FROM collection WHERE harvested_collection.collection_legacy_id = collection.collection_id);
ALTER TABLE harvested_collection DROP COLUMN collection_legacy_id;
UPDATE harvested_collection SET harvest_type = -1 WHERE harvest_type IS NULL;
UPDATE harvested_collection SET harvest_status = -1 WHERE harvest_status IS NULL;
CREATE INDEX harvested_collection_collection on harvested_collection(collection_id);

--Alter workspaceitem
ALTER TABLE workspaceitem RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE workspaceitem ADD COLUMN item_id UUID REFERENCES Item(uuid);
UPDATE workspaceitem SET item_id = (SELECT item.uuid FROM item WHERE workspaceitem.item_legacy_id = item.item_id);
ALTER TABLE workspaceitem DROP COLUMN item_legacy_id;
CREATE INDEX workspaceitem_item on workspaceitem(item_id);

ALTER TABLE workspaceitem RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE workspaceitem ADD COLUMN collection_id UUID REFERENCES Collection(uuid);
ALTER TABLE workspaceitem ADD CONSTRAINT workspaceitem_collection_id_fk FOREIGN KEY (collection_id) REFERENCES collection;
UPDATE workspaceitem SET collection_id = (SELECT collection.uuid FROM collection WHERE workspaceitem.collection_legacy_id = collection.collection_id);
ALTER TABLE workspaceitem DROP COLUMN collection_legacy_id;
CREATE INDEX workspaceitem_coll on workspaceitem(collection_id);

UPDATE workspaceitem SET multiple_titles = FALSE WHERE multiple_titles IS NULL;
UPDATE workspaceitem SET published_before = FALSE WHERE published_before IS NULL;
UPDATE workspaceitem SET multiple_files = FALSE WHERE multiple_files IS NULL;
UPDATE workspaceitem SET stage_reached = -1 WHERE stage_reached IS NULL;
UPDATE workspaceitem SET page_reached = -1 WHERE page_reached IS NULL;


ALTER TABLE epersongroup2workspaceitem RENAME COLUMN eperson_group_id to eperson_group_legacy_id;
ALTER TABLE epersongroup2workspaceitem ADD COLUMN eperson_group_id UUID REFERENCES epersongroup(uuid);
UPDATE epersongroup2workspaceitem SET eperson_group_id = (SELECT epersongroup.uuid FROM epersongroup WHERE epersongroup2workspaceitem.eperson_group_legacy_id = epersongroup.eperson_group_id);
ALTER TABLE epersongroup2workspaceitem DROP COLUMN eperson_group_legacy_id;
ALTER TABLE epersongroup2workspaceitem DROP COLUMN id;
ALTER TABLE epersongroup2workspaceitem ALTER COLUMN workspace_item_id SET NOT NULL;
ALTER TABLE epersongroup2workspaceitem ALTER COLUMN eperson_group_id SET NOT NULL;
ALTER TABLE epersongroup2workspaceitem add primary key (workspace_item_id,eperson_group_id);
CREATE INDEX epersongroup2workspaceitem_group on epersongroup2workspaceitem(eperson_group_id);

--Alter most_recent_checksum
ALTER TABLE most_recent_checksum RENAME COLUMN bitstream_id to bitstream_legacy_id;
ALTER TABLE most_recent_checksum ADD COLUMN bitstream_id UUID REFERENCES Bitstream(uuid);
UPDATE most_recent_checksum SET bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE most_recent_checksum.bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE most_recent_checksum DROP COLUMN bitstream_legacy_id;
UPDATE most_recent_checksum SET to_be_processed = FALSE WHERE to_be_processed IS NULL;
UPDATE most_recent_checksum SET matched_prev_checksum = FALSE WHERE matched_prev_checksum IS NULL;
CREATE INDEX most_recent_checksum_bitstream on most_recent_checksum(bitstream_id);

ALTER TABLE checksum_history RENAME COLUMN bitstream_id to bitstream_legacy_id;
ALTER TABLE checksum_history ADD COLUMN bitstream_id UUID REFERENCES Bitstream(uuid);
UPDATE checksum_history SET bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE checksum_history.bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE checksum_history DROP COLUMN bitstream_legacy_id;
CREATE INDEX checksum_history_bitstream on checksum_history(bitstream_id);

--Alter table doi
ALTER TABLE doi ADD COLUMN dspace_object UUID REFERENCES dspaceobject(uuid);
UPDATE doi SET dspace_object = (SELECT community.uuid FROM community WHERE doi.resource_id = community.community_id AND doi.resource_type_id = 4)  WHERE doi.resource_type_id = 4;
UPDATE doi SET dspace_object = (SELECT collection.uuid FROM collection WHERE doi.resource_id = collection.collection_id AND doi.resource_type_id = 3)  WHERE doi.resource_type_id = 3;
UPDATE doi SET dspace_object = (SELECT item.uuid FROM item WHERE doi.resource_id = item.item_id AND doi.resource_type_id = 2)  WHERE doi.resource_type_id = 2;
UPDATE doi SET dspace_object = (SELECT bundle.uuid FROM bundle WHERE doi.resource_id = bundle.bundle_id AND doi.resource_type_id = 1)  WHERE doi.resource_type_id = 1;
UPDATE doi SET dspace_object = (SELECT bitstream.uuid FROM bitstream WHERE doi.resource_id = bitstream.bitstream_id AND doi.resource_type_id = 0)  WHERE doi.resource_type_id = 0;
CREATE INDEX doi_object on doi(dspace_object);

UPDATE bitstreamformatregistry SET support_level = -1 WHERE support_level IS NULL;

UPDATE requestitem SET allfiles = FALSE WHERE allfiles IS NULL;
UPDATE requestitem SET accept_request = FALSE WHERE accept_request IS NULL;

UPDATE webapp SET isui = -1 WHERE isui IS NULL;
