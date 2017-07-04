--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

------------------------------------------------------
-- DS-3636 General index and caching perfomance fixes
------------------------------------------------------
-- Indexes on resourcepolicy, versionitem, group2groupcache, metadata and bundle2bitstream colums to improve searching efficiency

DROP INDEX IF EXISTS resourcepolicy_rptype_idx;
DROP INDEX IF EXISTS resourcepolicy_action_idx;
DROP INDEX IF EXISTS resourcepolicy_resource_type_id_idx;
DROP INDEX IF EXISTS resourcepolicy_resource_id_idx;
DROP INDEX IF EXISTS versionitem_item_id_idx;
DROP INDEX IF EXISTS versionitem_versionhistory_id_idx;
DROP INDEX IF EXISTS group2groupcache_parent_id_idx;
DROP INDEX IF EXISTS group2groupcache_child_id_idx;
DROP INDEX IF EXISTS metadatavalue_mf_place_idx;
DROP INDEX IF EXISTS bundle2bitstream_bitstream_order_idx;

CREATE INDEX resourcepolicy_rptype_idx ON resourcepolicy (rptype);
CREATE INDEX resourcepolicy_action_idx ON resourcepolicy (action_id);
CREATE INDEX resourcepolicy_resource_type_id_idx ON resourcepolicy(resource_type_id );
CREATE INDEX resourcepolicy_resource_id_idx ON resourcepolicy(resource_id );
CREATE INDEX versionitem_item_id_idx ON versionitem (item_id);
CREATE INDEX versionitem_versionhistory_id_idx ON versionitem (versionhistory_id);
CREATE INDEX group2groupcache_parent_id_idx ON group2groupcache (parent_id);
CREATE INDEX group2groupcache_child_id_idx ON group2groupcache (child_id);
CREATE INDEX bundle2bitstream_bitstream_order_idx ON bundle2bitstream (bitstream_order);
CREATE INDEX metadatavalue_mf_place_idx ON metadatavalue (metadata_field_id, place);