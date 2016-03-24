--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

/**
 * DS-2981: Create indexes for UUID fields for Oracle migration script
 * Author:  Mark H. Wood
 * Created: Mar 23, 2016
 */

CREATE INDEX EpersonGroup2Eperson_group on EpersonGroup2Eperson(eperson_group_id);
CREATE INDEX EpersonGroup2Eperson_person on EpersonGroup2Eperson(eperson_id);
CREATE INDEX Group2Group_parent on Group2Group(parent_id);
CREATE INDEX Group2Group_child on Group2Group(child_id);
CREATE INDEX Collecion2Item_collection on Collection2Item(collection_id);
CREATE INDEX Collecion2Item_item on Collection2Item(item_id);
CREATE INDEX Community2Community_parent on Community2Community(parent_comm_id);
CREATE INDEX Community2Community_child on Community2Community(child_comm_id);
CREATE INDEX community2collection_collectio on community2collection(collection_id);
CREATE INDEX community2collection_community on community2collection(community_id);
CREATE INDEX Group2GroupCache_parent on Group2GroupCache(parent_id);
CREATE INDEX Group2GroupCache_child on Group2GroupCache(child_id);
CREATE INDEX item2bundle_bundle on item2bundle(bundle_id);
CREATE INDEX item2bundle_item on item2bundle(item_id);
CREATE INDEX bundle2bitstream_bundle on bundle2bitstream(bundle_id);
CREATE INDEX bundle2bitstream_bitstream on bundle2bitstream(bitstream_id);
CREATE INDEX item_submitter on item(submitter_id);
CREATE INDEX item_collection on item(owning_collection);
CREATE INDEX bundle_primary on bundle(primary_bitstream_id);
CREATE INDEX Community_admin on Community(admin);
CREATE INDEX Community_bitstream on Community(logo_bitstream_id);
CREATE INDEX Collection_workflow1 on Collection(workflow_step_1);
CREATE INDEX Collection_workflow2 on Collection(workflow_step_2);
CREATE INDEX Collection_workflow3 on Collection(workflow_step_3);
CREATE INDEX Collection_submitter on Collection(submitter);
CREATE INDEX Collection_template on Collection(template_item_id);
CREATE INDEX Collection_bitstream on Collection(logo_bitstream_id);
CREATE INDEX resourcepolicy_person on resourcepolicy(eperson_id);
CREATE INDEX resourcepolicy_group on resourcepolicy(epersongroup_id);
CREATE INDEX resourcepolicy_object on resourcepolicy(dspace_object);
CREATE INDEX Subscription_person on Subscription(eperson_id);
CREATE INDEX Subscription_collection on Subscription(collection_id);
CREATE INDEX versionitem_person on versionitem(eperson_id);
CREATE INDEX versionitem_item on versionitem(item_id);
CREATE INDEX handle_object on handle(resource_id);
CREATE INDEX metadatavalue_object on metadatavalue(dspace_object_id);
-- CREATE INDEX metadatavalue_field on metadatavalue(metadata_field_id);
CREATE INDEX metadatavalue_field_object on metadatavalue(metadata_field_id, dspace_object_id);
CREATE INDEX harvested_item_item on harvested_item(item_id);
CREATE INDEX harvested_collection_collectio on harvested_collection(collection_id);
CREATE INDEX workspaceitem_item on workspaceitem(item_id);
CREATE INDEX workspaceitem_coll on workspaceitem(collection_id);
CREATE INDEX epersongroup2workspaceitem_gro on epersongroup2workspaceitem(eperson_group_id);
CREATE INDEX most_recent_checksum_bitstream on most_recent_checksum(bitstream_id);
CREATE INDEX checksum_history_bitstream on checksum_history(bitstream_id);
CREATE INDEX doi_object on doi(dspace_object);
