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
/
declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin
-- Note -> This index should actually be called "resourcepolicy_resource_type_id_idx", but this exceeds the identifier length of 30.
  execute immediate 'DROP INDEX policy_resource_type_id_idx';
  exception
  when index_not_exists then null;
end;
/
declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX versionitem_item_id_idx';
  exception
  when index_not_exists then null;
end;
/
declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin
-- Note -> This index should actually be called "versionitem_versionhistory_id_idx" by convention, but this exceeds the identifier length of 30.
  execute immediate 'DROP INDEX versionitem_history_id_idx';
  exception
  when index_not_exists then null;
end;
/
declare
    index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin
-- Note -> This index should actually be called "bundle2bitstream_bitstream_order_idx" by convention, but this exceeds the identifier length of 30.
  execute immediate 'DROP INDEX bundle2bitstream_bit_order_idx';
  exception
  when index_not_exists then null;
end;
/
declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX metadatavalue_mf_place_idx';
  exception
  when index_not_exists then null;
end;
/
declare
  index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX resourcepolicy_type_id_idx';
  exception
  when index_not_exists then null;
end;
/

CREATE INDEX resourcepolicy_action_idx ON resourcepolicy (action_id);

CREATE INDEX versionitem_item_id_idx ON versionitem (item_id);
CREATE INDEX metadatavalue_mf_place_idx ON metadatavalue (place);

-- The following indexes do NOT adhere to previously used index naming conventions, but these would exceed the ORACLE defined identifier length of 30 characters

-- Note -> This index should actually be called "versionitem_versionhistory_id_idx" by convention, but this exceeds the identifier length of 30.
CREATE INDEX versionitem_history_id_idx ON versionitem (versionhistory_id);
-- Note -> This index should actually be called "bundle2bitstream_bitstream_order_idx" by convention, but this exceeds the identifier length of 30.
CREATE INDEX bundle2bitstream_bit_order_idx ON bundle2bitstream (bitstream_order);
-- Note -> This index should actually be called "resourcepolicy_resource_type_id_idx" by convention, but this exceeds the identifier length of 30.
CREATE INDEX policy_resource_type_id_idx ON resourcepolicy(resource_type_id );