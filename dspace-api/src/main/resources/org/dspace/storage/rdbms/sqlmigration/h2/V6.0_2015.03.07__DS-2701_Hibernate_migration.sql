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
    uuid            uuid NOT NULL PRIMARY KEY
);

CREATE TABLE site
(
    uuid            uuid NOT NULL PRIMARY KEY REFERENCES dspaceobject(uuid)

);

ALTER TABLE eperson ADD COLUMN uuid UUID DEFAULT random_uuid();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM eperson;
ALTER TABLE eperson ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE eperson ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE eperson ADD CONSTRAINT eperson_id_unique UNIQUE(uuid);
ALTER TABLE eperson ADD PRIMARY KEY (uuid);
ALTER TABLE eperson ALTER COLUMN eperson_id SET NULL;



UPDATE metadatavalue SET text_value='Administrator'
  WHERE resource_type_id=6 AND resource_id=1;
UPDATE metadatavalue SET text_value='Anonymous'
  WHERE resource_type_id=6 AND resource_id=0;

ALTER TABLE epersongroup ADD COLUMN uuid UUID DEFAULT random_uuid();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM epersongroup;
ALTER TABLE epersongroup ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE epersongroup ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE epersongroup ADD CONSTRAINT epersongroup_id_unique UNIQUE(uuid);
ALTER TABLE epersongroup ADD PRIMARY KEY (uuid);
ALTER TABLE epersongroup ALTER COLUMN eperson_group_id SET NULL;

ALTER TABLE item ADD COLUMN uuid UUID DEFAULT random_uuid();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM item;
ALTER TABLE item ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE item ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE item ADD CONSTRAINT item_id_unique UNIQUE(uuid);
ALTER TABLE item ADD PRIMARY KEY (uuid);
ALTER TABLE item ALTER COLUMN item_id SET NULL;

ALTER TABLE community ADD COLUMN uuid UUID DEFAULT random_uuid();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM community;
ALTER TABLE community ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE community ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE community ADD CONSTRAINT community_id_unique UNIQUE(uuid);
ALTER TABLE community ADD PRIMARY KEY (uuid);
ALTER TABLE community ALTER COLUMN community_id SET NULL;


ALTER TABLE collection ADD COLUMN uuid UUID DEFAULT random_uuid();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM collection;
ALTER TABLE collection ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE collection ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE collection ADD CONSTRAINT collection_id_unique UNIQUE(uuid);
ALTER TABLE collection ADD PRIMARY KEY (uuid);
ALTER TABLE collection ALTER COLUMN collection_id SET NULL;

ALTER TABLE bundle ADD COLUMN uuid UUID DEFAULT random_uuid();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM bundle;
ALTER TABLE bundle ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE bundle ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE bundle ADD CONSTRAINT bundle_id_unique UNIQUE(uuid);
ALTER TABLE bundle ADD PRIMARY KEY (uuid);
ALTER TABLE bundle ALTER COLUMN bundle_id SET NULL;

ALTER TABLE bitstream ADD COLUMN uuid UUID DEFAULT random_uuid();
INSERT INTO dspaceobject  (uuid) SELECT uuid FROM bitstream;
ALTER TABLE bitstream ADD FOREIGN KEY (uuid) REFERENCES dspaceobject;
ALTER TABLE bitstream ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE bitstream ADD CONSTRAINT bitstream_id_unique UNIQUE(uuid);
ALTER TABLE bitstream ADD PRIMARY KEY (uuid);
ALTER TABLE bitstream ALTER COLUMN bitstream_id SET NULL;

-- Migrate EPersonGroup2EPerson table
ALTER TABLE EPersonGroup2EPerson ALTER COLUMN  eperson_group_id rename to eperson_group_legacy_id;
ALTER TABLE EPersonGroup2EPerson ALTER COLUMN eperson_id rename to eperson_legacy_id;
ALTER TABLE EPersonGroup2EPerson ADD COLUMN eperson_group_id UUID NOT NULL;
ALTER TABLE EPersonGroup2EPerson ADD CONSTRAINT EPersonGroup2EPerson_eperson_group_id_fk FOREIGN KEY (eperson_group_id) REFERENCES EpersonGroup;
ALTER TABLE EPersonGroup2EPerson ADD COLUMN eperson_id UUID NOT NULL;
ALTER TABLE EPersonGroup2EPerson ADD CONSTRAINT EPersonGroup2EPerson_eperson_id_fk FOREIGN KEY (eperson_id) REFERENCES Eperson;
UPDATE EPersonGroup2EPerson SET eperson_group_id = (SELECT EPersonGroup.uuid FROM EpersonGroup WHERE EPersonGroup2EPerson.eperson_group_legacy_id = EPersonGroup.eperson_group_id);
UPDATE EPersonGroup2EPerson SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE EPersonGroup2EPerson.eperson_legacy_id = eperson.eperson_id);
ALTER TABLE EPersonGroup2EPerson DROP COLUMN eperson_group_legacy_id;
ALTER TABLE EPersonGroup2EPerson DROP COLUMN eperson_legacy_id;
ALTER TABLE epersongroup2eperson DROP COLUMN id;
ALTER TABLE EPersonGroup2EPerson add primary key (eperson_group_id,eperson_id);


-- Migrate GROUP2GROUP table
ALTER TABLE Group2Group ALTER COLUMN parent_id rename to parent_legacy_id;
ALTER TABLE Group2Group ALTER COLUMN child_id rename to child_legacy_id;
ALTER TABLE Group2Group ADD COLUMN parent_id UUID NOT NULL;
ALTER TABLE Group2Group ADD CONSTRAINT Group2Group_parent_id_fk FOREIGN KEY (parent_id) REFERENCES EpersonGroup;
ALTER TABLE Group2Group ADD COLUMN child_id UUID NOT NULL;
ALTER TABLE Group2Group ADD CONSTRAINT Group2Group_child_id_fk FOREIGN KEY (child_id) REFERENCES EpersonGroup;
UPDATE Group2Group SET parent_id = (SELECT EPersonGroup.uuid FROM EpersonGroup WHERE Group2Group.parent_legacy_id = EPersonGroup.eperson_group_id);
UPDATE Group2Group SET child_id = (SELECT EpersonGroup.uuid FROM EpersonGroup WHERE Group2Group.child_legacy_id = EpersonGroup.eperson_group_id);
ALTER TABLE Group2Group DROP COLUMN parent_legacy_id;
ALTER TABLE Group2Group DROP COLUMN child_legacy_id;
ALTER TABLE Group2Group DROP COLUMN id;
ALTER TABLE Group2Group add primary key (parent_id,child_id);

-- Migrate collection2item
ALTER TABLE Collection2Item ALTER COLUMN collection_id rename to collection_legacy_id;
ALTER TABLE Collection2Item ALTER COLUMN item_id rename to item_legacy_id;
ALTER TABLE Collection2Item ADD COLUMN collection_id UUID NOT NULL;
ALTER TABLE Collection2Item ADD CONSTRAINT Collection2Item_collection_id_fk FOREIGN KEY (collection_id) REFERENCES Collection;
ALTER TABLE Collection2Item ADD COLUMN item_id UUID NOT NULL;
ALTER TABLE Collection2Item ADD CONSTRAINT Collection2Item_item_id_fk FOREIGN KEY (item_id) REFERENCES Item;
UPDATE Collection2Item SET collection_id = (SELECT Collection.uuid FROM Collection WHERE Collection2Item.collection_legacy_id = Collection.collection_id);
UPDATE Collection2Item SET item_id = (SELECT Item.uuid FROM Item WHERE Collection2Item.item_legacy_id = Item.item_id);
ALTER TABLE Collection2Item DROP COLUMN collection_legacy_id;
ALTER TABLE Collection2Item DROP COLUMN item_legacy_id;
ALTER TABLE Collection2Item DROP COLUMN id;
ALTER TABLE Collection2Item add primary key (collection_id,item_id);

-- Migrate Community2Community
ALTER TABLE Community2Community ALTER COLUMN parent_comm_id rename to parent_legacy_id;
ALTER TABLE Community2Community ALTER COLUMN child_comm_id rename to child_legacy_id;
ALTER TABLE Community2Community ADD COLUMN parent_comm_id UUID NOT NULL;
ALTER TABLE Community2Community ADD CONSTRAINT Community2Community_parent_comm_id_fk FOREIGN KEY (parent_comm_id) REFERENCES Community;
ALTER TABLE Community2Community ADD COLUMN child_comm_id UUID NOT NULL;
ALTER TABLE Community2Community ADD CONSTRAINT Community2Community_child_comm_id_fk FOREIGN KEY (child_comm_id) REFERENCES Community;
UPDATE Community2Community SET parent_comm_id = (SELECT EPersonGroup.uuid FROM EpersonGroup WHERE Community2Community.parent_legacy_id = EPersonGroup.eperson_group_id);
UPDATE Community2Community SET child_comm_id = (SELECT EpersonGroup.uuid FROM EpersonGroup WHERE Community2Community.child_legacy_id = EpersonGroup.eperson_group_id);
ALTER TABLE Community2Community DROP COLUMN parent_legacy_id;
ALTER TABLE Community2Community DROP COLUMN child_legacy_id;
ALTER TABLE Community2Community DROP COLUMN id;
ALTER TABLE Community2Community add primary key (parent_comm_id,child_comm_id);

-- Migrate community2collection
ALTER TABLE community2collection ALTER COLUMN collection_id rename to collection_legacy_id;
ALTER TABLE community2collection ALTER COLUMN community_id rename to community_legacy_id;
ALTER TABLE community2collection ADD COLUMN collection_id UUID NOT NULL;
ALTER TABLE community2collection ADD CONSTRAINT community2collection_collection_id_fk FOREIGN KEY (collection_id) REFERENCES Collection;
ALTER TABLE community2collection ADD COLUMN community_id UUID NOT NULL;
ALTER TABLE community2collection ADD CONSTRAINT community2collection_community_id_fk FOREIGN KEY (community_id) REFERENCES Community;
UPDATE community2collection SET collection_id = (SELECT Collection.uuid FROM Collection WHERE community2collection.collection_legacy_id = Collection.collection_id);
UPDATE community2collection SET community_id = (SELECT Community.uuid FROM Community WHERE community2collection.community_legacy_id = Community.community_id);
ALTER TABLE community2collection DROP COLUMN collection_legacy_id;
ALTER TABLE community2collection DROP COLUMN community_legacy_id;
ALTER TABLE community2collection DROP COLUMN id;
ALTER TABLE community2collection add primary key (collection_id,community_id);


-- Migrate Group2GroupCache table
ALTER TABLE Group2GroupCache ALTER COLUMN parent_id rename to parent_legacy_id;
ALTER TABLE Group2GroupCache ALTER COLUMN child_id rename to child_legacy_id;
ALTER TABLE Group2GroupCache ADD COLUMN parent_id UUID NOT NULL;
ALTER TABLE Group2GroupCache ADD CONSTRAINT Group2GroupCache_parent_id_fk FOREIGN KEY (parent_id) REFERENCES EpersonGroup;
ALTER TABLE Group2GroupCache ADD COLUMN child_id UUID NOT NULL;
ALTER TABLE Group2GroupCache ADD CONSTRAINT Group2GroupCache_child_id_fk FOREIGN KEY (child_id) REFERENCES EpersonGroup;
UPDATE Group2GroupCache SET parent_id = (SELECT EPersonGroup.uuid FROM EpersonGroup WHERE Group2GroupCache.parent_legacy_id = EPersonGroup.eperson_group_id);
UPDATE Group2GroupCache SET child_id = (SELECT EpersonGroup.uuid FROM EpersonGroup WHERE Group2GroupCache.child_legacy_id = EpersonGroup.eperson_group_id);
ALTER TABLE Group2GroupCache DROP COLUMN parent_legacy_id;
ALTER TABLE Group2GroupCache DROP COLUMN child_legacy_id;
ALTER TABLE Group2GroupCache DROP COLUMN id;
ALTER TABLE Group2GroupCache add primary key (parent_id,child_id);

-- Migrate Item2Bundle
ALTER TABLE item2bundle ALTER COLUMN bundle_id rename to bundle_legacy_id;
ALTER TABLE item2bundle ALTER COLUMN item_id rename to item_legacy_id;
ALTER TABLE item2bundle ADD COLUMN bundle_id UUID NOT NULL;
ALTER TABLE item2bundle ADD CONSTRAINT item2bundle_bundle_id_fk FOREIGN KEY (bundle_id) REFERENCES bundle;
ALTER TABLE item2bundle ADD COLUMN item_id UUID NOT NULL;
ALTER TABLE item2bundle ADD CONSTRAINT item2bundle_item_id_fk FOREIGN KEY (item_id) REFERENCES item;
UPDATE item2bundle SET bundle_id = (SELECT bundle.uuid FROM bundle WHERE item2bundle.bundle_legacy_id = bundle.bundle_id);
UPDATE item2bundle SET item_id = (SELECT item.uuid FROM item WHERE item2bundle.item_legacy_id = item.item_id);
ALTER TABLE item2bundle DROP COLUMN bundle_legacy_id;
ALTER TABLE item2bundle DROP COLUMN item_legacy_id;
ALTER TABLE item2bundle DROP COLUMN id;
ALTER TABLE item2bundle add primary key (item_id,bundle_id);

--Migrate Bundle2Bitsteam
ALTER TABLE bundle2bitstream ALTER COLUMN bundle_id rename to bundle_legacy_id;
ALTER TABLE bundle2bitstream ALTER COLUMN bitstream_id rename to bitstream_legacy_id;
ALTER TABLE bundle2bitstream ADD COLUMN bundle_id UUID NOT NULL;
ALTER TABLE bundle2bitstream ADD CONSTRAINT bundle2bitstream_bundle_id_fk FOREIGN KEY (bundle_id) REFERENCES bundle;
ALTER TABLE bundle2bitstream ADD COLUMN bitstream_id UUID NOT NULL;
ALTER TABLE bundle2bitstream ADD CONSTRAINT bundle2bitstream_bitstream_id_fk FOREIGN KEY (bitstream_id) REFERENCES bitstream;
UPDATE bundle2bitstream SET bundle_id = (SELECT bundle.uuid FROM bundle WHERE bundle2bitstream.bundle_legacy_id = bundle.bundle_id);
UPDATE bundle2bitstream SET bitstream_id = (SELECT bitstream.uuid FROM bitstream WHERE bundle2bitstream.bitstream_legacy_id = bitstream.bitstream_id);
ALTER TABLE bundle2bitstream DROP COLUMN bundle_legacy_id;
ALTER TABLE bundle2bitstream DROP COLUMN bitstream_legacy_id;
ALTER TABLE bundle2bitstream DROP COLUMN id;
ALTER TABLE bundle2bitstream add primary key (bitstream_id,bundle_id);


-- Migrate item
ALTER TABLE item ALTER COLUMN submitter_id rename to submitter_id_legacy_id;
ALTER TABLE item ADD COLUMN submitter_id UUID;
ALTER TABLE item ADD CONSTRAINT item_submitter_id_fk FOREIGN KEY (submitter_id) REFERENCES EPerson;
UPDATE item SET submitter_id = (SELECT eperson.uuid FROM eperson WHERE item.submitter_id_legacy_id = eperson.eperson_id);
ALTER TABLE item DROP COLUMN submitter_id_legacy_id;

ALTER TABLE item ALTER COLUMN owning_collection rename to owning_collection_legacy;
ALTER TABLE item ADD COLUMN owning_collection UUID;
ALTER TABLE item ADD CONSTRAINT item_owning_collection_fk FOREIGN KEY (owning_collection) REFERENCES Collection;
UPDATE item SET owning_collection = (SELECT Collection.uuid FROM Collection WHERE item.owning_collection_legacy = collection.collection_id);
ALTER TABLE item DROP COLUMN owning_collection_legacy;

-- Migrate bundle
ALTER TABLE bundle ALTER COLUMN primary_bitstream_id rename to primary_bitstream_legacy_id;
ALTER TABLE bundle ADD COLUMN primary_bitstream_id UUID;
ALTER TABLE bundle ADD CONSTRAINT bundle_primary_bitstream_id_fk FOREIGN KEY (primary_bitstream_id) REFERENCES Bitstream;
UPDATE bundle SET primary_bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE bundle.primary_bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE bundle DROP COLUMN primary_bitstream_legacy_id;


-- Migrate community references
ALTER TABLE Community ALTER COLUMN admin rename to admin_legacy;
ALTER TABLE Community ADD COLUMN admin UUID;
ALTER TABLE Community ADD CONSTRAINT Community_admin_uuid_fk FOREIGN KEY (admin) REFERENCES EPersonGroup;
UPDATE Community SET admin = (SELECT EPersonGroup.uuid FROM EPersonGroup WHERE Community.admin_legacy = EPersonGroup.eperson_group_id);
ALTER TABLE Community DROP COLUMN admin_legacy;
ALTER TABLE Community ALTER COLUMN logo_bitstream_id rename to logo_bitstream_legacy_id;
ALTER TABLE Community ADD COLUMN logo_bitstream_id UUID;
UPDATE Community SET logo_bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE Community.logo_bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE Community DROP COLUMN logo_bitstream_legacy_id;


--Migrate Collection references
ALTER TABLE Collection ALTER COLUMN workflow_step_1 rename to workflow_step_1_legacy;
ALTER TABLE Collection ALTER COLUMN workflow_step_2 rename to workflow_step_2_legacy;
ALTER TABLE Collection ALTER COLUMN workflow_step_3 rename to workflow_step_3_legacy;
ALTER TABLE Collection ALTER COLUMN submitter rename to submitter_legacy;
ALTER TABLE Collection ALTER COLUMN template_item_id rename to template_item_legacy_id;
ALTER TABLE Collection ALTER COLUMN logo_bitstream_id rename to logo_bitstream_legacy_id;
ALTER TABLE Collection ALTER COLUMN admin rename to admin_legacy;
ALTER TABLE Collection ADD COLUMN workflow_step_1 UUID;
ALTER TABLE Collection ADD CONSTRAINT collection_workflow_step_1_fk FOREIGN KEY (workflow_step_1) REFERENCES EPersonGroup;
ALTER TABLE Collection ADD COLUMN workflow_step_2 UUID;
ALTER TABLE Collection ADD CONSTRAINT collection_workflow_step_2_fk FOREIGN KEY (workflow_step_2) REFERENCES EPersonGroup;
ALTER TABLE Collection ADD COLUMN workflow_step_3 UUID;
ALTER TABLE Collection ADD CONSTRAINT collection_workflow_step_3_fk FOREIGN KEY (workflow_step_3) REFERENCES EPersonGroup;
ALTER TABLE Collection ADD COLUMN submitter UUID;
ALTER TABLE Collection ADD COLUMN template_item_id UUID;
ALTER TABLE Collection ADD COLUMN logo_bitstream_id UUID;
ALTER TABLE Collection ADD COLUMN admin UUID;
ALTER TABLE Collection ADD CONSTRAINT collection_admin_fk FOREIGN KEY (admin) REFERENCES EPersonGroup;
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
ALTER TABLE ResourcePolicy ALTER COLUMN eperson_id rename to eperson_id_legacy_id;
ALTER TABLE ResourcePolicy ADD COLUMN eperson_id UUID;
ALTER TABLE ResourcePolicy ADD CONSTRAINT ResourcePolicy_eperson_id_fk FOREIGN KEY (eperson_id) REFERENCES EPerson;
UPDATE ResourcePolicy SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE ResourcePolicy.eperson_id_legacy_id = eperson.eperson_id);
ALTER TABLE ResourcePolicy DROP COLUMN eperson_id_legacy_id;
ALTER TABLE ResourcePolicy ALTER COLUMN epersongroup_id rename to epersongroup_id_legacy_id;
ALTER TABLE ResourcePolicy ADD COLUMN epersongroup_id UUID;
ALTER TABLE ResourcePolicy ADD CONSTRAINT ResourcePolicy_epersongroup_id_fk FOREIGN KEY (epersongroup_id) REFERENCES EPersonGroup;
UPDATE ResourcePolicy SET epersongroup_id = (SELECT epersongroup.uuid FROM epersongroup WHERE ResourcePolicy.epersongroup_id_legacy_id = epersongroup.eperson_group_id);
ALTER TABLE ResourcePolicy DROP COLUMN epersongroup_id_legacy_id;

ALTER TABLE ResourcePolicy ADD COLUMN dspace_object UUID;
ALTER TABLE ResourcePolicy ADD CONSTRAINT ResourcePolicy_dspace_object_fk FOREIGN KEY (dspace_object) REFERENCES dspaceobject;
UPDATE ResourcePolicy SET dspace_object = (SELECT eperson.uuid FROM eperson WHERE ResourcePolicy.resource_id = eperson.eperson_id AND ResourcePolicy.resource_type_id = 7);
UPDATE ResourcePolicy SET dspace_object = (SELECT epersongroup.uuid FROM epersongroup WHERE ResourcePolicy.resource_id = epersongroup.eperson_group_id AND ResourcePolicy.resource_type_id = 6);
UPDATE ResourcePolicy SET dspace_object = (SELECT community.uuid FROM community WHERE ResourcePolicy.resource_id = community.community_id AND ResourcePolicy.resource_type_id = 4);
UPDATE ResourcePolicy SET dspace_object = (SELECT collection.uuid FROM collection WHERE ResourcePolicy.resource_id = collection.collection_id AND ResourcePolicy.resource_type_id = 3);
UPDATE ResourcePolicy SET dspace_object = (SELECT item.uuid FROM item WHERE ResourcePolicy.resource_id = item.item_id AND ResourcePolicy.resource_type_id = 2);
UPDATE ResourcePolicy SET dspace_object = (SELECT bundle.uuid FROM bundle WHERE ResourcePolicy.resource_id = bundle.bundle_id AND ResourcePolicy.resource_type_id = 1);
UPDATE ResourcePolicy SET dspace_object = (SELECT bitstream.uuid FROM bitstream WHERE ResourcePolicy.resource_id = bitstream.bitstream_id AND ResourcePolicy.resource_type_id = 0);



-- Migrate Subscription
ALTER TABLE Subscription ALTER COLUMN eperson_id rename to eperson_legacy_id;
ALTER TABLE Subscription ADD COLUMN eperson_id UUID;
ALTER TABLE Subscription ADD CONSTRAINT Subscription_eperson_id_fk FOREIGN KEY (eperson_id) REFERENCES EPerson;
UPDATE Subscription SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE Subscription.eperson_legacy_id = eperson.eperson_id);
ALTER TABLE Subscription DROP COLUMN eperson_legacy_id;
ALTER TABLE Subscription ALTER COLUMN collection_id rename to collection_legacy_id;
ALTER TABLE Subscription ADD COLUMN collection_id UUID;
ALTER TABLE Subscription ADD CONSTRAINT Subscription_collection_id_fk FOREIGN KEY (collection_id) REFERENCES Collection;
UPDATE Subscription SET collection_id = (SELECT collection.uuid FROM collection WHERE Subscription.collection_legacy_id = collection.collection_id);
ALTER TABLE Subscription DROP COLUMN collection_legacy_id;

-- Migrate versionitem
ALTER TABLE versionitem ALTER COLUMN eperson_id rename to eperson_legacy_id;
ALTER TABLE versionitem ADD COLUMN eperson_id UUID;
ALTER TABLE versionitem ADD CONSTRAINT versionitem_eperson_id_fk FOREIGN KEY (eperson_id) REFERENCES EPerson;
UPDATE versionitem SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE versionitem.eperson_legacy_id = eperson.eperson_id);
ALTER TABLE versionitem DROP COLUMN eperson_legacy_id;

ALTER TABLE versionitem ALTER COLUMN item_id rename to item_legacy_id;
ALTER TABLE versionitem ADD COLUMN item_id UUID;
ALTER TABLE versionitem ADD CONSTRAINT versionitem_item_id_fk FOREIGN KEY (item_id) REFERENCES Item;
UPDATE versionitem SET item_id = (SELECT item.uuid FROM item WHERE versionitem.item_legacy_id = item.item_id);
ALTER TABLE versionitem DROP COLUMN item_legacy_id;

-- Migrate handle table
ALTER TABLE handle ALTER COLUMN resource_id rename to resource_legacy_id;
ALTER TABLE handle ADD COLUMN resource_id UUID;
ALTER TABLE handle ADD CONSTRAINT handle_resource_id_fk FOREIGN KEY (resource_id) REFERENCES dspaceobject;
UPDATE handle SET resource_id = (SELECT community.uuid FROM community WHERE handle.resource_legacy_id = community.community_id AND handle.resource_type_id = 4);
UPDATE handle SET resource_id = (SELECT collection.uuid FROM collection WHERE handle.resource_legacy_id = collection.collection_id AND handle.resource_type_id = 3);
UPDATE handle SET resource_id = (SELECT item.uuid FROM item WHERE handle.resource_legacy_id = item.item_id AND handle.resource_type_id = 2);



-- Migrate metadata value table
DROP VIEW dcvalue;

ALTER TABLE metadatavalue ADD COLUMN dspace_object_id UUID;
ALTER TABLE metadatavalue ADD CONSTRAINT metadatavalue_dspace_object_id_fk FOREIGN KEY (dspace_object_id) REFERENCES dspaceobject;
UPDATE metadatavalue SET dspace_object_id = (SELECT eperson.uuid FROM eperson WHERE metadatavalue.resource_id = eperson.eperson_id AND metadatavalue.resource_type_id = 7);
UPDATE metadatavalue SET dspace_object_id = (SELECT epersongroup.uuid FROM epersongroup WHERE metadatavalue.resource_id = epersongroup.eperson_group_id AND metadatavalue.resource_type_id = 6);
UPDATE metadatavalue SET dspace_object_id = (SELECT community.uuid FROM community WHERE metadatavalue.resource_id = community.community_id AND metadatavalue.resource_type_id = 4);
UPDATE metadatavalue SET dspace_object_id = (SELECT collection.uuid FROM collection WHERE metadatavalue.resource_id = collection.collection_id AND metadatavalue.resource_type_id = 3);
UPDATE metadatavalue SET dspace_object_id = (SELECT item.uuid FROM item WHERE metadatavalue.resource_id = item.item_id AND metadatavalue.resource_type_id = 2);
UPDATE metadatavalue SET dspace_object_id = (SELECT bundle.uuid FROM bundle WHERE metadatavalue.resource_id = bundle.bundle_id AND metadatavalue.resource_type_id = 1);
UPDATE metadatavalue SET dspace_object_id = (SELECT bitstream.uuid FROM bitstream WHERE metadatavalue.resource_id = bitstream.bitstream_id AND metadatavalue.resource_type_id = 0);
DROP INDEX metadatavalue_item_idx;
DROP INDEX metadatavalue_item_idx2;
ALTER TABLE metadatavalue DROP COLUMN IF EXISTS resource_id;
ALTER TABLE metadatavalue DROP COLUMN resource_type_id;

-- Alter harvested item
ALTER TABLE harvested_item ALTER COLUMN item_id rename to item_legacy_id;
ALTER TABLE harvested_item ADD COLUMN item_id UUID;
ALTER TABLE harvested_item ADD CONSTRAINT harvested_item_item_id_fk FOREIGN KEY (item_id) REFERENCES Item;
UPDATE harvested_item SET item_id = (SELECT item.uuid FROM item WHERE harvested_item.item_legacy_id = item.item_id);
ALTER TABLE harvested_item DROP COLUMN item_legacy_id;

-- Alter harvested collection
ALTER TABLE harvested_collection ALTER COLUMN collection_id rename to collection_legacy_id;
ALTER TABLE harvested_collection ADD COLUMN collection_id UUID;
ALTER TABLE harvested_collection ADD CONSTRAINT harvested_collection_collection_id_fk FOREIGN KEY (collection_id) REFERENCES collection;
UPDATE harvested_collection SET collection_id = (SELECT collection.uuid FROM collection WHERE harvested_collection.collection_legacy_id = collection.collection_id);
ALTER TABLE harvested_collection DROP COLUMN collection_legacy_id;


--Alter workspaceitem
ALTER TABLE workspaceitem ALTER COLUMN item_id rename to item_legacy_id;
ALTER TABLE workspaceitem ADD COLUMN item_id UUID;
ALTER TABLE workspaceitem ADD CONSTRAINT workspaceitem_item_id_fk FOREIGN KEY (item_id) REFERENCES Item;
UPDATE workspaceitem SET item_id = (SELECT item.uuid FROM item WHERE workspaceitem.item_legacy_id = item.item_id);
ALTER TABLE workspaceitem DROP COLUMN item_legacy_id;

ALTER TABLE workspaceitem ALTER COLUMN collection_id rename to collection_legacy_id;
ALTER TABLE workspaceitem ADD COLUMN collection_id UUID;
ALTER TABLE workspaceitem ADD CONSTRAINT workspaceitem_collection_id_fk FOREIGN KEY (collection_id) REFERENCES collection;
UPDATE workspaceitem SET collection_id = (SELECT collection.uuid FROM collection WHERE workspaceitem.collection_legacy_id = collection.collection_id);
ALTER TABLE workspaceitem DROP COLUMN collection_legacy_id;


ALTER TABLE epersongroup2workspaceitem ALTER COLUMN eperson_group_id rename to eperson_group_legacy_id;
ALTER TABLE epersongroup2workspaceitem ADD COLUMN eperson_group_id UUID;
ALTER TABLE epersongroup2workspaceitem ADD CONSTRAINT epersongroup2workspaceitem_eperson_group_id_fk FOREIGN KEY (eperson_group_id) REFERENCES epersongroup;
UPDATE epersongroup2workspaceitem SET eperson_group_id = (SELECT epersongroup.uuid FROM epersongroup WHERE epersongroup2workspaceitem.eperson_group_legacy_id = epersongroup.eperson_group_id);
ALTER TABLE epersongroup2workspaceitem DROP COLUMN eperson_group_legacy_id;
ALTER TABLE epersongroup2workspaceitem DROP COLUMN id;
ALTER TABLE epersongroup2workspaceitem ALTER COLUMN workspace_item_id SET NOT NULL;
ALTER TABLE epersongroup2workspaceitem ALTER COLUMN eperson_group_id SET NOT NULL;
ALTER TABLE epersongroup2workspaceitem add primary key (workspace_item_id,eperson_group_id);



--Alter most_recent_checksum
ALTER TABLE most_recent_checksum ALTER COLUMN bitstream_id rename to bitstream_legacy_id;
ALTER TABLE most_recent_checksum ADD COLUMN bitstream_id UUID;
ALTER TABLE most_recent_checksum ADD CONSTRAINT most_recent_checksum_bitstream_id_fk FOREIGN KEY (bitstream_id) REFERENCES Bitstream;
UPDATE most_recent_checksum SET bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE most_recent_checksum.bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE most_recent_checksum DROP COLUMN bitstream_legacy_id;

ALTER TABLE checksum_history ALTER COLUMN bitstream_id rename to bitstream_legacy_id;
ALTER TABLE checksum_history ADD COLUMN bitstream_id UUID;
ALTER TABLE checksum_history ADD CONSTRAINT checksum_history_id_fk FOREIGN KEY (bitstream_id) REFERENCES Bitstream;
UPDATE checksum_history SET bitstream_id = (SELECT Bitstream.uuid FROM Bitstream WHERE checksum_history.bitstream_legacy_id = Bitstream.bitstream_id);
ALTER TABLE checksum_history DROP COLUMN bitstream_legacy_id;

--Alter table doi
ALTER TABLE doi ADD COLUMN dspace_object UUID;
ALTER TABLE doi ADD CONSTRAINT doi_dspace_object_fk FOREIGN KEY (dspace_object) REFERENCES dspaceobject;
UPDATE doi SET dspace_object = (SELECT community.uuid FROM community WHERE doi.resource_id = community.community_id AND doi.resource_type_id = 4)  WHERE doi.resource_type_id = 4;
UPDATE doi SET dspace_object = (SELECT collection.uuid FROM collection WHERE doi.resource_id = collection.collection_id AND doi.resource_type_id = 3)  WHERE doi.resource_type_id = 3;
UPDATE doi SET dspace_object = (SELECT item.uuid FROM item WHERE doi.resource_id = item.item_id AND doi.resource_type_id = 2)  WHERE doi.resource_type_id = 2;
UPDATE doi SET dspace_object = (SELECT bundle.uuid FROM bundle WHERE doi.resource_id = bundle.bundle_id AND doi.resource_type_id = 1)  WHERE doi.resource_type_id = 1;
UPDATE doi SET dspace_object = (SELECT bitstream.uuid FROM bitstream WHERE doi.resource_id = bitstream.bitstream_id AND doi.resource_type_id = 0)  WHERE doi.resource_type_id = 0;

