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

declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX resourcepolicy_action_idx';
  exception
  when index_not_exists then null;
end;

declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX resourcepolicy_resource_type_id_idx';
  exception
  when index_not_exists then null;
end;

declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX resourcepolicy_resource_id_idx';
  exception
  when index_not_exists then null;
end;

declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX versionitem_item_id_idx';
  exception
  when index_not_exists then null;
end;

declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX versionitem_versionhistory_id_idx';
  exception
  when index_not_exists then null;
end;

declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX group2groupcache_parent_id_idx';
  exception
  when index_not_exists then null;
end;

declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX group2groupcache_child_id_idx';
  exception
  when index_not_exists then null;
end;

declare
    index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX bundle2bitstream_bitstream_order_idx';
  exception
  when index_not_exists then null;
end;

declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX metadatavalue_mf_place_idx';
  exception
  when index_not_exists then null;
end;


CREATE INDEX resourcepolicy_action_idx ON resourcepolicy (action_id);
CREATE INDEX resourcepolicy_resource_type_id_idx ON resourcepolicy(resource_type_id );
CREATE INDEX resourcepolicy_resource_id_idx ON resourcepolicy(resource_id );
CREATE INDEX versionitem_item_id_idx ON versionitem (item_id);
CREATE INDEX versionitem_versionhistory_id_idx ON versionitem (versionhistory_id);
CREATE INDEX group2groupcache_parent_id_idx ON group2groupcache (parent_id);
CREATE INDEX group2groupcache_child_id_idx ON group2groupcache (child_id);
CREATE INDEX bundle2bitstream_bitstream_order_idx ON bundle2bitstream (bitstream_order);
CREATE INDEX metadatavalue_mf_place_idx ON metadatavalue (metadata_field_id, place);