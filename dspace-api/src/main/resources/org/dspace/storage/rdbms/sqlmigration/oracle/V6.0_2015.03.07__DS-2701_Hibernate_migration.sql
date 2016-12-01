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
    uuid            RAW(16) NOT NULL  PRIMARY KEY
);

CREATE TABLE site
(
    uuid            RAW(16) NOT NULL PRIMARY KEY REFERENCES dspaceobject(uuid)
);

ALTER TABLE eperson ADD uuid RAW(16) DEFAULT SYS_GUID();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM eperson;
ALTER TABLE eperson ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE eperson MODIFY uuid NOT NULL;
ALTER TABLE eperson ADD CONSTRAINT eperson_id_unique PRIMARY KEY (uuid);
UPDATE eperson SET require_certificate = '0' WHERE require_certificate IS NULL;
UPDATE eperson SET self_registered = '0' WHERE self_registered IS NULL;



UPDATE metadatavalue SET text_value='Administrator'
  WHERE resource_type_id=6 AND resource_id=1;
UPDATE metadatavalue SET text_value='Anonymous'
  WHERE resource_type_id=6 AND resource_id=0;

ALTER TABLE epersongroup ADD uuid RAW(16) DEFAULT SYS_GUID();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM epersongroup;
ALTER TABLE epersongroup ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE epersongroup MODIFY uuid NOT NULL;
ALTER TABLE epersongroup ADD CONSTRAINT epersongroup_id_unique PRIMARY KEY (uuid);

ALTER TABLE item ADD uuid RAW(16) DEFAULT SYS_GUID();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM item;
ALTER TABLE item ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE item MODIFY uuid NOT NULL;
ALTER TABLE item ADD CONSTRAINT item_id_unique PRIMARY KEY (uuid);

ALTER TABLE community ADD uuid RAW(16) DEFAULT SYS_GUID();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM community;
ALTER TABLE community ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE community MODIFY uuid NOT NULL;
ALTER TABLE community ADD CONSTRAINT community_id_unique PRIMARY KEY (uuid);


ALTER TABLE collection ADD uuid RAW(16) DEFAULT SYS_GUID();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM collection;
ALTER TABLE collection ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE collection MODIFY uuid NOT NULL;
ALTER TABLE collection ADD CONSTRAINT collection_id_unique PRIMARY KEY (uuid);

ALTER TABLE bundle ADD uuid RAW(16) DEFAULT SYS_GUID();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM bundle;
ALTER TABLE bundle ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE bundle MODIFY uuid NOT NULL;
ALTER TABLE bundle ADD CONSTRAINT bundle_id_unique PRIMARY KEY (uuid);

ALTER TABLE bitstream ADD uuid RAW(16) DEFAULT SYS_GUID();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM bitstream;
ALTER TABLE bitstream ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE bitstream MODIFY uuid NOT NULL;
ALTER TABLE bitstream ADD CONSTRAINT bitstream_id_unique PRIMARY KEY (uuid);
UPDATE bitstream SET sequence_id = -1 WHERE sequence_id IS NULL;
UPDATE bitstream SET size_bytes = -1 WHERE size_bytes IS NULL;
UPDATE bitstream SET deleted = '0' WHERE deleted IS NULL;
UPDATE bitstream SET store_number = -1 WHERE store_number IS NULL;

-- Migrate EPersonGroup2EPerson table
ALTER TABLE EPersonGroup2EPerson RENAME COLUMN eperson_group_id to eperson_group_legacy_id;
ALTER TABLE EPersonGroup2EPerson RENAME COLUMN eperson_id to eperson_legacy_id;
ALTER TABLE EPersonGroup2EPerson ADD eperson_group_id RAW(16) REFERENCES EpersonGroup(uuid);
ALTER TABLE EPersonGroup2EPerson ADD eperson_id RAW(16) REFERENCES Eperson(uuid);
CREATE INDEX EpersonGroup2Eperson_group on EpersonGroup2Eperson(eperson_group_id);
CREATE INDEX EpersonGroup2Eperson_person on EpersonGroup2Eperson(eperson_id);
UPDATE EPersonGroup2EPerson SET eperson_group_id = (SELECT EPersonGroup.uuid FROM EpersonGroup WHERE EPersonGroup2EPerson.eperson_group_legacy_id = EPersonGroup.eperson_group_id);
UPDATE EPersonGroup2EPerson SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE EPersonGroup2EPerson.eperson_legacy_id = eperson.eperson_id);
ALTER TABLE EPersonGroup2EPerson MODIFY eperson_group_id NOT NULL;
ALTER TABLE EPersonGroup2EPerson MODIFY eperson_id NOT NULL;
ALTER TABLE EPersonGroup2EPerson DROP COLUMN eperson_group_legacy_id;
ALTER TABLE EPersonGroup2EPerson DROP COLUMN eperson_legacy_id;
ALTER TABLE epersongroup2eperson DROP COLUMN id;
ALTER TABLE EPersonGroup2EPerson add CONSTRAINT EPersonGroup2EPerson_unique primary key (eperson_group_id,eperson_id);

-- Migrate GROUP2GROUP table
ALTER TABLE Group2Group RENAME COLUMN parent_id to parent_legacy_id;
ALTER TABLE Group2Group RENAME COLUMN child_id to child_legacy_id;
ALTER TABLE Group2Group ADD parent_id RAW(16) REFERENCES EpersonGroup(uuid);
ALTER TABLE Group2Group ADD child_id RAW(16) REFERENCES EpersonGroup(uuid);
CREATE INDEX Group2Group_parent on Group2Group(parent_id);
CREATE INDEX Group2Group_child on Group2Group(child_id);
UPDATE Group2Group SET parent_id = (SELECT EPersonGroup.uuid FROM EpersonGroup WHERE Group2Group.parent_legacy_id = EPersonGroup.eperson_group_id);
UPDATE Group2Group SET child_id = (SELECT EpersonGroup.uuid FROM EpersonGroup WHERE Group2Group.child_legacy_id = EpersonGroup.eperson_group_id);
ALTER TABLE Group2Group MODIFY parent_id NOT NULL;
ALTER TABLE Group2Group MODIFY child_id NOT NULL;
ALTER TABLE Group2Group DROP COLUMN parent_legacy_id;
ALTER TABLE Group2Group DROP COLUMN child_legacy_id;
ALTER TABLE Group2Group DROP COLUMN id;
ALTER TABLE Group2Group add CONSTRAINT Group2Group_unique primary key (parent_id,child_id);

-- Migrate collection2item
ALTER TABLE Collection2Item RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE Collection2Item RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE Collection2Item ADD collection_id RAW(16) REFERENCES Collection(uuid);
ALTER TABLE Collection2Item ADD item_id RAW(16) REFERENCES Item(uuid);
CREATE INDEX Collecion2Item_collection on Collection2Item(collection_id);
CREATE INDEX Collecion2Item_item on Collection2Item(item_id);
UPDATE Collection2Item SET collection_id = (SELECT Collection.uuid FROM Collection WHERE Collection2Item.collection_legacy_id = Collection.collection_id);
UPDATE Collection2Item SET item_id = (SELECT Item.uuid FROM Item WHERE Collection2Item.item_legacy_id = Item.item_id);
ALTER TABLE Collection2Item MODIFY collection_id NOT NULL;
ALTER TABLE Collection2Item MODIFY item_id NOT NULL;
ALTER TABLE Collection2Item DROP COLUMN collection_legacy_id;
ALTER TABLE Collection2Item DROP COLUMN item_legacy_id;
ALTER TABLE Collection2Item DROP COLUMN id;
-- Magic query that will delete all duplicate collection item_id references from the database (if we don't do this the primary key creation will fail)
DELETE FROM collection2item WHERE rowid NOT IN (SELECT MIN(rowid) FROM collection2item GROUP BY collection_id,item_id);
ALTER TABLE Collection2Item add CONSTRAINT collection2item_unique primary key (collection_id,item_id);

-- Migrate Community2Community
ALTER TABLE Community2Community RENAME COLUMN parent_comm_id to parent_legacy_id;
ALTER TABLE Community2Community RENAME COLUMN child_comm_id to child_legacy_id;
ALTER TABLE Community2Community ADD parent_comm_id RAW(16) REFERENCES Community(uuid);
ALTER TABLE Community2Community ADD child_comm_id RAW(16) REFERENCES Community(uuid);
CREATE INDEX Community2Community_parent on Community2Community(parent_comm_id);
CREATE INDEX Community2Community_child on Community2Community(child_comm_id);
UPDATE Community2Community SET parent_comm_id = (SELECT Community.uuid FROM Community WHERE Community2Community.parent_legacy_id = Community.community_id);
UPDATE Community2Community SET child_comm_id = (SELECT Community.uuid FROM Community WHERE Community2Community.child_legacy_id = Community.community_id);
ALTER TABLE Community2Community MODIFY parent_comm_id NOT NULL;
ALTER TABLE Community2Community MODIFY child_comm_id NOT NULL;
ALTER TABLE Community2Community DROP COLUMN parent_legacy_id;
ALTER TABLE Community2Community DROP COLUMN child_legacy_id;
ALTER TABLE Community2Community DROP COLUMN id;
ALTER TABLE Community2Community add CONSTRAINT Community2Community_unique primary key (parent_comm_id,child_comm_id);

-- Migrate community2collection
ALTER TABLE community2collection RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE community2collection RENAME COLUMN community_id to community_legacy_id;
ALTER TABLE community2collection ADD collection_id RAW(16) REFERENCES Collection(uuid);
ALTER TABLE community2collection ADD community_id RAW(16) REFERENCES Community(uuid);
CREATE INDEX community2collection_collectio on community2collection(collection_id);
CREATE INDEX community2collection_community on community2collection(community_id);
UPDATE community2collection SET collection_id = (SELECT Collection.uuid FROM Collection WHERE community2collection.collection_legacy_id = Collection.collection_id);
UPDATE community2collection SET community_id = (SELECT Community.uuid FROM Community WHERE community2collection.community_legacy_id = Community.community_id);
ALTER TABLE community2collection MODIFY collection_id NOT NULL;
ALTER TABLE community2collection MODIFY community_id NOT NULL;
ALTER TABLE community2collection DROP COLUMN collection_legacy_id;
ALTER TABLE community2collection DROP COLUMN community_legacy_id;
ALTER TABLE community2collection DROP COLUMN id;
ALTER TABLE community2collection add CONSTRAINT community2collection_unique primary key (collection_id,community_id);


-- Migrate Group2GroupCache table
ALTER TABLE Group2GroupCache RENAME COLUMN parent_id to parent_legacy_id;
ALTER TABLE Group2GroupCache RENAME COLUMN child_id to child_legacy_id;
ALTER TABLE Group2GroupCache ADD parent_id RAW(16) REFERENCES EpersonGroup(uuid);
ALTER TABLE Group2GroupCache ADD child_id RAW(16) REFERENCES EpersonGroup(uuid);
CREATE INDEX Group2GroupCache_parent on Group2GroupCache(parent_id);
CREATE INDEX Group2GroupCache_child on Group2GroupCache(child_id);
UPDATE Group2GroupCache SET parent_id = (SELECT EPersonGroup.uuid FROM EpersonGroup WHERE Group2GroupCache.parent_legacy_id = EPersonGroup.eperson_group_id);
UPDATE Group2GroupCache SET child_id = (SELECT EpersonGroup.uuid FROM EpersonGroup WHERE Group2GroupCache.child_legacy_id = EpersonGroup.eperson_group_id);
ALTER TABLE Group2GroupCache MODIFY parent_id NOT NULL;
ALTER TABLE Group2GroupCache MODIFY child_id NOT NULL;
ALTER TABLE Group2GroupCache DROP COLUMN parent_legacy_id;
ALTER TABLE Group2GroupCache DROP COLUMN child_legacy_id;
ALTER TABLE Group2GroupCache DROP COLUMN id;
ALTER TABLE Group2GroupCache add CONSTRAINT Group2GroupCache_unique primary key (parent_id,child_id);

-- Migrate Item2Bundle
ALTER TABLE item2bundle RENAME COLUMN bundle_id to bundle_legacy_id;
ALTER TABLE item2bundle RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE item2bundle ADD bundle_id RAW(16) REFERENCES Bundle(uuid);
ALTER TABLE item2bundle ADD item_id RAW(16) REFERENCES Item(uuid);
CREATE INDEX item2bundle_bundle on item2bundle(bundle_id);
CREATE INDEX item2bundle_item on item2bundle(item_id);
UPDATE item2bundle SET bundle_id = (SELECT Bundle.uuid FROM Bundle WHERE item2bundle.bundle_legacy_id = Bundle.bundle_id);
UPDATE item2bundle SET item_id = (SELECT Item.uuid FROM Item WHERE item2bundle.item_legacy_id = Item.item_id);
ALTER TABLE item2bundle MODIFY bundle_id NOT NULL;
ALTER TABLE item2bundle MODIFY item_id NOT NULL;
ALTER TABLE item2bundle DROP COLUMN bundle_legacy_id;
ALTER TABLE item2bundle DROP COLUMN item_legacy_id;
ALTER TABLE item2bundle DROP COLUMN id;
ALTER TABLE item2bundle add CONSTRAINT item2bundle_unique primary key (bundle_id,item_id);

--Migrate Bundle2Bitsteam
ALTER TABLE bundle2bitstream RENAME COLUMN bundle_id to bundle_legacy_id;
ALTER TABLE bundle2bitstream RENAME COLUMN bitstream_id to bitstream_legacy_id;
ALTER TABLE bundle2bitstream ADD bundle_id RAW(16) REFERENCES Bundle(uuid);
ALTER TABLE bundle2bitstream ADD bitstream_id RAW(16) REFERENCES Bitstream(uuid);
CREATE INDEX bundle2bitstream_bundle on bundle2bitstream(bundle_id);
CREATE INDEX bundle2bitstream_bitstream on bundle2bitstream(bitstream_id);
UPDATE bundle2bitstream SET bundle_id = (SELECT bundle.uuid FROM bundle WHERE bundle2bitstream.bundle_legacy_id = bundle.bundle_id);
UPDATE bundle2bitstream SET bitstream_id = (SELECT bitstream.uuid FROM bitstream WHERE bundle2bitstream.bitstream_legacy_id = bitstream.bitstream_id);
ALTER TABLE bundle2bitstream RENAME COLUMN bitstream_order to bitstream_order_legacy;
ALTER TABLE bundle2bitstream ADD bitstream_order INTEGER;
MERGE INTO     bundle2bitstream     dst
USING  (       SELECT     ROWID     AS r_id
                   ,      ROW_NUMBER () OVER ( PARTITION BY  bundle_id
                                           ORDER BY          bitstream_order_legacy, bitstream_id
                            )         AS new_order
            FROM     bundle2bitstream
       )                 src
ON     (dst.ROWID     = src.r_id)
WHEN MATCHED THEN UPDATE
SET    dst.bitstream_order       = (src.new_order-1)
;
ALTER TABLE bundle2bitstream MODIFY bundle_id NOT NULL;
ALTER TABLE bundle2bitstream MODIFY bitstream_id NOT NULL;
ALTER TABLE bundle2bitstream DROP COLUMN bundle_legacy_id;
ALTER TABLE bundle2bitstream DROP COLUMN bitstream_legacy_id;
ALTER TABLE bundle2bitstream DROP COLUMN id;
ALTER TABLE bundle2bitstream add CONSTRAINT bundle2bitstream_unique primary key (bitstream_id,bundle_id,bitstream_order);


-- Migrate item
ALTER TABLE item RENAME COLUMN submitter_id to submitter_id_legacy_id;
ALTER TABLE item ADD submitter_id RAW(16) REFERENCES EPerson(uuid);
CREATE INDEX item_submitter on item(submitter_id);
UPDATE item SET submitter_id = (SELECT eperson.uuid FROM eperson WHERE item.submitter_id_legacy_id = eperson.eperson_id);
ALTER TABLE item DROP COLUMN submitter_id_legacy_id;

ALTER TABLE item RENAME COLUMN owning_collection to owning_collection_legacy;
ALTER TABLE item ADD owning_collection RAW(16) REFERENCES Collection(uuid);
CREATE INDEX item_collection on item(owning_collection);
UPDATE item SET owning_collection = (SELECT Collection.uuid FROM Collection WHERE item.owning_collection_legacy = collection.collection_id);
ALTER TABLE item DROP COLUMN owning_collection_legacy;

UPDATE item SET in_archive = '0' WHERE in_archive IS NULL;
UPDATE item SET discoverable = '0' WHERE discoverable IS NULL;
UPDATE item SET withdrawn = '0' WHERE withdrawn IS NULL;

-- Migrate bundle
ALTER TABLE bundle RENAME COLUMN primary_bitstream_id to primary_bitstream_legacy_id;
ALTER TABLE bundle ADD primary_bitstream_id RAW(16) REFERENCES Bitstream(uuid);
CREATE INDEX bundle_primary on bundle(primary_bitstream_id);
UPDATE bundle SET primary_bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE bundle.primary_bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE bundle DROP COLUMN primary_bitstream_legacy_id;


-- Migrate community references
ALTER TABLE Community RENAME COLUMN admin to admin_legacy;
ALTER TABLE Community ADD admin RAW(16) REFERENCES EPersonGroup(uuid);
CREATE INDEX Community_admin on Community(admin);
UPDATE Community SET admin = (SELECT EPersonGroup.uuid FROM EPersonGroup WHERE Community.admin_legacy = EPersonGroup.eperson_group_id);
ALTER TABLE Community DROP COLUMN admin_legacy;

ALTER TABLE Community RENAME COLUMN logo_bitstream_id to logo_bitstream_legacy_id;
ALTER TABLE Community ADD logo_bitstream_id RAW(16) REFERENCES Bitstream(uuid);
CREATE INDEX Community_bitstream on Community(logo_bitstream_id);
UPDATE Community SET logo_bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE Community.logo_bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE Community DROP COLUMN logo_bitstream_legacy_id;


--Migrate Collection references
ALTER TABLE Collection RENAME COLUMN workflow_step_1 to workflow_step_1_legacy;
ALTER TABLE Collection RENAME COLUMN workflow_step_2 to workflow_step_2_legacy;
ALTER TABLE Collection RENAME COLUMN workflow_step_3 to workflow_step_3_legacy;
ALTER TABLE Collection RENAME COLUMN submitter to submitter_legacy;
ALTER TABLE Collection RENAME COLUMN template_item_id to template_item_legacy_id;
ALTER TABLE Collection RENAME COLUMN logo_bitstream_id to logo_bitstream_legacy_id;
ALTER TABLE Collection RENAME COLUMN admin to admin_legacy;
ALTER TABLE Collection ADD workflow_step_1 RAW(16) REFERENCES EPersonGroup(uuid);
ALTER TABLE Collection ADD workflow_step_2 RAW(16) REFERENCES EPersonGroup(uuid);
ALTER TABLE Collection ADD workflow_step_3 RAW(16) REFERENCES EPersonGroup(uuid);
ALTER TABLE Collection ADD submitter RAW(16) REFERENCES EPersonGroup(uuid);
ALTER TABLE Collection ADD template_item_id RAW(16);
ALTER TABLE Collection ADD logo_bitstream_id RAW(16);
ALTER TABLE Collection ADD admin RAW(16) REFERENCES EPersonGroup(uuid);
CREATE INDEX Collection_workflow1 on Collection(workflow_step_1);
CREATE INDEX Collection_workflow2 on Collection(workflow_step_2);
CREATE INDEX Collection_workflow3 on Collection(workflow_step_3);
CREATE INDEX Collection_submitter on Collection(submitter);
CREATE INDEX Collection_template on Collection(template_item_id);
CREATE INDEX Collection_bitstream on Collection(logo_bitstream_id);
UPDATE Collection SET workflow_step_1 = (SELECT EPersonGroup.uuid FROM EPersonGroup WHERE Collection.workflow_step_1_legacy = EPersonGroup.eperson_group_id);
UPDATE Collection SET workflow_step_2 = (SELECT EPersonGroup.uuid FROM EPersonGroup WHERE Collection.workflow_step_2_legacy = EPersonGroup.eperson_group_id);
UPDATE Collection SET workflow_step_3 = (SELECT EPersonGroup.uuid FROM EPersonGroup WHERE Collection.workflow_step_3_legacy = EPersonGroup.eperson_group_id);
UPDATE Collection SET submitter = (SELECT EPersonGroup.uuid FROM EPersonGroup WHERE Collection.submitter_legacy = EPersonGroup.eperson_group_id);
UPDATE Collection SET template_item_id = (SELECT Item.uuid FROM Item WHERE Collection.template_item_legacy_id = Item.item_id);
UPDATE Collection SET logo_bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE Collection.logo_bitstream_legacy_id = Bitstream.bitstream_id);
UPDATE Collection SET admin = (SELECT EPersonGroup.uuid FROM EPersonGroup WHERE Collection.admin_legacy = EPersonGroup.eperson_group_id);
ALTER TABLE Collection DROP COLUMN workflow_step_1_legacy;
ALTER TABLE Collection DROP COLUMN workflow_step_2_legacy;
ALTER TABLE Collection DROP COLUMN workflow_step_3_legacy;
ALTER TABLE Collection DROP COLUMN submitter_legacy;
ALTER TABLE Collection DROP COLUMN template_item_legacy_id;
ALTER TABLE Collection DROP COLUMN logo_bitstream_legacy_id;
ALTER TABLE Collection DROP COLUMN admin_legacy;


-- Migrate resource policy references
ALTER TABLE ResourcePolicy RENAME COLUMN eperson_id to eperson_id_legacy_id;
ALTER TABLE ResourcePolicy ADD eperson_id RAW(16) REFERENCES EPerson(uuid);
CREATE INDEX resourcepolicy_person on resourcepolicy(eperson_id);
UPDATE ResourcePolicy SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE ResourcePolicy.eperson_id_legacy_id = eperson.eperson_id);
ALTER TABLE ResourcePolicy DROP COLUMN eperson_id_legacy_id;

ALTER TABLE ResourcePolicy RENAME COLUMN epersongroup_id to epersongroup_id_legacy_id;
ALTER TABLE ResourcePolicy ADD epersongroup_id RAW(16) REFERENCES EPersonGroup(uuid);
CREATE INDEX resourcepolicy_group on resourcepolicy(epersongroup_id);
UPDATE ResourcePolicy SET epersongroup_id = (SELECT epersongroup.uuid FROM epersongroup WHERE ResourcePolicy.epersongroup_id_legacy_id = epersongroup.eperson_group_id);
ALTER TABLE ResourcePolicy DROP COLUMN epersongroup_id_legacy_id;

ALTER TABLE ResourcePolicy ADD dspace_object RAW(16) REFERENCES dspaceobject(uuid);
CREATE INDEX resourcepolicy_object on resourcepolicy(dspace_object);
UPDATE ResourcePolicy SET dspace_object = (SELECT eperson.uuid FROM eperson WHERE ResourcePolicy.resource_id = eperson.eperson_id AND ResourcePolicy.resource_type_id = 7) WHERE ResourcePolicy.resource_type_id = 7;
UPDATE ResourcePolicy SET dspace_object = (SELECT epersongroup.uuid FROM epersongroup WHERE ResourcePolicy.resource_id = epersongroup.eperson_group_id AND ResourcePolicy.resource_type_id = 6)  WHERE ResourcePolicy.resource_type_id = 6;
UPDATE ResourcePolicy SET dspace_object = (SELECT community.uuid FROM community WHERE ResourcePolicy.resource_id = community.community_id AND ResourcePolicy.resource_type_id = 4)  WHERE ResourcePolicy.resource_type_id = 4;
UPDATE ResourcePolicy SET dspace_object = (SELECT collection.uuid FROM collection WHERE ResourcePolicy.resource_id = collection.collection_id AND ResourcePolicy.resource_type_id = 3)  WHERE ResourcePolicy.resource_type_id = 3;
UPDATE ResourcePolicy SET dspace_object = (SELECT item.uuid FROM item WHERE ResourcePolicy.resource_id = item.item_id AND ResourcePolicy.resource_type_id = 2)  WHERE ResourcePolicy.resource_type_id = 2;
UPDATE ResourcePolicy SET dspace_object = (SELECT bundle.uuid FROM bundle WHERE ResourcePolicy.resource_id = bundle.bundle_id AND ResourcePolicy.resource_type_id = 1)  WHERE ResourcePolicy.resource_type_id = 1;
UPDATE ResourcePolicy SET dspace_object = (SELECT bitstream.uuid FROM bitstream WHERE ResourcePolicy.resource_id = bitstream.bitstream_id AND ResourcePolicy.resource_type_id = 0)  WHERE ResourcePolicy.resource_type_id = 0;
UPDATE resourcepolicy SET resource_type_id = -1 WHERE resource_type_id IS NULL;
UPDATE resourcepolicy SET action_id = -1 WHERE action_id IS NULL;


-- Migrate Subscription
ALTER TABLE Subscription RENAME COLUMN eperson_id to eperson_legacy_id;
ALTER TABLE Subscription ADD eperson_id RAW(16) REFERENCES EPerson(uuid);
CREATE INDEX Subscription_person on Subscription(eperson_id);
UPDATE Subscription SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE Subscription.eperson_legacy_id = eperson.eperson_id);
ALTER TABLE Subscription DROP COLUMN eperson_legacy_id;

ALTER TABLE Subscription RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE Subscription ADD collection_id RAW(16) REFERENCES Collection(uuid);
CREATE INDEX Subscription_collection on Subscription(collection_id);
UPDATE Subscription SET collection_id = (SELECT collection.uuid FROM collection WHERE Subscription.collection_legacy_id = collection.collection_id);
ALTER TABLE Subscription DROP COLUMN collection_legacy_id;


-- Migrate versionitem
ALTER TABLE versionitem RENAME COLUMN eperson_id to eperson_legacy_id;
ALTER TABLE versionitem ADD eperson_id RAW(16) REFERENCES EPerson(uuid);
CREATE INDEX versionitem_person on versionitem(eperson_id);
UPDATE versionitem SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE versionitem.eperson_legacy_id = eperson.eperson_id);
ALTER TABLE versionitem DROP COLUMN eperson_legacy_id;

ALTER TABLE versionitem RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE versionitem ADD item_id RAW(16) REFERENCES Item(uuid);
CREATE INDEX versionitem_item on versionitem(item_id);
UPDATE versionitem SET item_id = (SELECT item.uuid FROM item WHERE versionitem.item_legacy_id = item.item_id);
ALTER TABLE versionitem DROP COLUMN item_legacy_id;
UPDATE versionitem SET version_number = -1 WHERE version_number IS NULL;

-- Migrate handle table
ALTER TABLE handle RENAME COLUMN resource_id to resource_legacy_id;
ALTER TABLE handle ADD resource_id RAW(16) REFERENCES dspaceobject(uuid);
CREATE INDEX handle_object on handle(resource_id);
UPDATE handle SET resource_id = (SELECT community.uuid FROM community WHERE handle.resource_legacy_id = community.community_id AND handle.resource_type_id = 4);
UPDATE handle SET resource_id = (SELECT collection.uuid FROM collection WHERE handle.resource_legacy_id = collection.collection_id AND handle.resource_type_id = 3);
UPDATE handle SET resource_id = (SELECT item.uuid FROM item WHERE handle.resource_legacy_id = item.item_id AND handle.resource_type_id = 2);

-- Migrate metadata value table
DROP VIEW dcvalue;

ALTER TABLE metadatavalue ADD dspace_object_id RAW(16) REFERENCES dspaceobject(uuid);
-- CREATE INDEX metadatavalue_field on metadatavalue(metadata_field_id);
CREATE INDEX metadatavalue_object on metadatavalue(dspace_object_id);
CREATE INDEX metadatavalue_field_object on metadatavalue(metadata_field_id, dspace_object_id);
UPDATE metadatavalue SET dspace_object_id = (SELECT eperson.uuid FROM eperson WHERE metadatavalue.resource_id = eperson.eperson_id AND metadatavalue.resource_type_id = 7) WHERE metadatavalue.resource_type_id= 7;
UPDATE metadatavalue SET dspace_object_id = (SELECT epersongroup.uuid FROM epersongroup WHERE metadatavalue.resource_id = epersongroup.eperson_group_id AND metadatavalue.resource_type_id = 6) WHERE metadatavalue.resource_type_id= 6;
UPDATE metadatavalue SET dspace_object_id = (SELECT community.uuid FROM community WHERE metadatavalue.resource_id = community.community_id AND metadatavalue.resource_type_id = 4) WHERE metadatavalue.resource_type_id= 4;
UPDATE metadatavalue SET dspace_object_id = (SELECT collection.uuid FROM collection WHERE metadatavalue.resource_id = collection.collection_id AND metadatavalue.resource_type_id = 3) WHERE metadatavalue.resource_type_id= 3;
UPDATE metadatavalue SET dspace_object_id = (SELECT item.uuid FROM item WHERE metadatavalue.resource_id = item.item_id AND metadatavalue.resource_type_id = 2) WHERE metadatavalue.resource_type_id= 2;
UPDATE metadatavalue SET dspace_object_id = (SELECT bundle.uuid FROM bundle WHERE metadatavalue.resource_id = bundle.bundle_id AND metadatavalue.resource_type_id = 1) WHERE metadatavalue.resource_type_id= 1;
UPDATE metadatavalue SET dspace_object_id = (SELECT bitstream.uuid FROM bitstream WHERE metadatavalue.resource_id = bitstream.bitstream_id AND metadatavalue.resource_type_id = 0) WHERE metadatavalue.resource_type_id= 0;
DROP INDEX metadatavalue_item_idx;
DROP INDEX metadatavalue_item_idx2;
ALTER TABLE metadatavalue DROP COLUMN resource_id;
ALTER TABLE metadatavalue DROP COLUMN resource_type_id;
UPDATE MetadataValue SET confidence = -1 WHERE confidence IS NULL;
UPDATE metadatavalue SET place = -1 WHERE place IS NULL;

-- Alter harvested item
ALTER TABLE harvested_item RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE harvested_item ADD item_id RAW(16) REFERENCES item(uuid);
CREATE INDEX harvested_item_item on harvested_item(item_id);
UPDATE harvested_item SET item_id = (SELECT item.uuid FROM item WHERE harvested_item.item_legacy_id = item.item_id);
ALTER TABLE harvested_item DROP COLUMN item_legacy_id;

-- Alter harvested collection
ALTER TABLE harvested_collection RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE harvested_collection ADD collection_id RAW(16) REFERENCES Collection(uuid);
CREATE INDEX harvested_collection_collectio on harvested_collection(collection_id);
UPDATE harvested_collection SET collection_id = (SELECT collection.uuid FROM collection WHERE harvested_collection.collection_legacy_id = collection.collection_id);
ALTER TABLE harvested_collection DROP COLUMN collection_legacy_id;

UPDATE harvested_collection SET harvest_type = -1 WHERE harvest_type IS NULL;
UPDATE harvested_collection SET harvest_status = -1 WHERE harvest_status IS NULL;


--Alter workspaceitem
ALTER TABLE workspaceitem RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE workspaceitem ADD item_id RAW(16) REFERENCES Item(uuid);
CREATE INDEX workspaceitem_item on workspaceitem(item_id);
UPDATE workspaceitem SET item_id = (SELECT item.uuid FROM item WHERE workspaceitem.item_legacy_id = item.item_id);
ALTER TABLE workspaceitem DROP COLUMN item_legacy_id;

ALTER TABLE workspaceitem RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE workspaceitem ADD collection_id RAW(16) REFERENCES Collection(uuid);
CREATE INDEX workspaceitem_coll on workspaceitem(collection_id);
UPDATE workspaceitem SET collection_id = (SELECT collection.uuid FROM collection WHERE workspaceitem.collection_legacy_id = collection.collection_id);
ALTER TABLE workspaceitem DROP COLUMN collection_legacy_id;

UPDATE workspaceitem SET multiple_titles = '0' WHERE multiple_titles IS NULL;
UPDATE workspaceitem SET published_before = '0' WHERE published_before IS NULL;
UPDATE workspaceitem SET multiple_files = '0' WHERE multiple_files IS NULL;
UPDATE workspaceitem SET stage_reached = -1 WHERE stage_reached IS NULL;
UPDATE workspaceitem SET page_reached = -1 WHERE page_reached IS NULL;

--Alter epersongroup2workspaceitem
ALTER TABLE epersongroup2workspaceitem RENAME COLUMN eperson_group_id to eperson_group_legacy_id;
ALTER TABLE epersongroup2workspaceitem ADD eperson_group_id RAW(16) REFERENCES epersongroup(uuid);
CREATE INDEX epersongroup2workspaceitem_gro on epersongroup2workspaceitem(eperson_group_id);
UPDATE epersongroup2workspaceitem SET eperson_group_id = (SELECT epersongroup.uuid FROM epersongroup WHERE epersongroup2workspaceitem.eperson_group_legacy_id = epersongroup.eperson_group_id);
ALTER TABLE epersongroup2workspaceitem DROP COLUMN eperson_group_legacy_id;

ALTER TABLE epersongroup2workspaceitem DROP COLUMN id;
ALTER TABLE epersongroup2workspaceitem MODIFY workspace_item_id NOT NULL;
ALTER TABLE epersongroup2workspaceitem MODIFY eperson_group_id NOT NULL;
ALTER TABLE epersongroup2workspaceitem add CONSTRAINT  epersongroup2wsitem_unqiue primary key (workspace_item_id,eperson_group_id);

--Alter most_recent_checksum
ALTER TABLE most_recent_checksum RENAME COLUMN bitstream_id to bitstream_legacy_id;
ALTER TABLE most_recent_checksum ADD bitstream_id RAW(16) REFERENCES Bitstream(uuid);
CREATE INDEX most_recent_checksum_bitstream on most_recent_checksum(bitstream_id);
UPDATE most_recent_checksum SET bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE most_recent_checksum.bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE most_recent_checksum DROP COLUMN bitstream_legacy_id;

UPDATE most_recent_checksum SET to_be_processed = '0' WHERE to_be_processed IS NULL;
UPDATE most_recent_checksum SET matched_prev_checksum = '0' WHERE matched_prev_checksum IS NULL;

--Alter checksum_history
ALTER TABLE checksum_history RENAME COLUMN bitstream_id to bitstream_legacy_id;
ALTER TABLE checksum_history ADD bitstream_id RAW(16) REFERENCES Bitstream(uuid);
CREATE INDEX checksum_history_bitstream on checksum_history(bitstream_id);
UPDATE checksum_history SET bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE checksum_history.bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE checksum_history DROP COLUMN bitstream_legacy_id;

RENAME checksum_history_seq TO checksum_history_check_id_seq;

--Alter table doi
ALTER TABLE doi ADD dspace_object RAW(16) REFERENCES dspaceobject(uuid);
CREATE INDEX doi_object on doi(dspace_object);
UPDATE doi SET dspace_object = (SELECT community.uuid FROM community WHERE doi.resource_id = community.community_id AND doi.resource_type_id = 4)  WHERE doi.resource_type_id = 4;
UPDATE doi SET dspace_object = (SELECT collection.uuid FROM collection WHERE doi.resource_id = collection.collection_id AND doi.resource_type_id = 3)  WHERE doi.resource_type_id = 3;
UPDATE doi SET dspace_object = (SELECT item.uuid FROM item WHERE doi.resource_id = item.item_id AND doi.resource_type_id = 2)  WHERE doi.resource_type_id = 2;
UPDATE doi SET dspace_object = (SELECT bundle.uuid FROM bundle WHERE doi.resource_id = bundle.bundle_id AND doi.resource_type_id = 1)  WHERE doi.resource_type_id = 1;
UPDATE doi SET dspace_object = (SELECT bitstream.uuid FROM bitstream WHERE doi.resource_id = bitstream.bitstream_id AND doi.resource_type_id = 0)  WHERE doi.resource_type_id = 0;

--Update table bitstreamformatregistry
UPDATE bitstreamformatregistry SET support_level = -1 WHERE support_level IS NULL;

--Update table requestitem
UPDATE requestitem SET allfiles = '0' WHERE allfiles IS NULL;
UPDATE requestitem SET accept_request = '0' WHERE accept_request IS NULL;

--Update table webapp
UPDATE webapp SET isui = -1 WHERE isui IS NULL;
