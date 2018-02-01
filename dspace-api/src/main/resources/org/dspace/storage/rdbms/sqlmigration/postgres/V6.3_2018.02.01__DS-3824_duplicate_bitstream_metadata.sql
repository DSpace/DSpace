--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

----------------------------------------------------------------------------------
-- DS-3824 : Bitstream metadata was duplicated when a new version was created
----------------------------------------------------------------------------------

DELETE FROM metadatavalue WHERE metadata_value_id IN (
  SELECT metadata_value_id FROM (
      SELECT metadata_value_id, ROW_NUMBER() OVER (
          PARTITION BY text_value, dspace_object_id ORDER BY metadata_value_id)
          AS row_num FROM metadatavalue) m WHERE m.row_num > 1
) AND dspace_object_id IN (SELECT uuid FROM bitstream);