--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

------------------------------------------------------
-- DS-3563 Missing database index on metadatavalue.resource_type_id
------------------------------------------------------
-- Create an index on the metadata value resource_type_id column so that it can be searched efficiently.
declare
    index_not_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT(index_not_exists, -1418);
begin

  execute immediate 'DROP INDEX metadatavalue_type_id_idx';
  exception
  when index_not_exists then null;
end;
/
CREATE INDEX metadatavalue_type_id_idx ON metadatavalue (resource_type_id);