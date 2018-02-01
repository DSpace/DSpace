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


DELETE FROM metadatavalue
  WHERE  rowid not in (
    SELECT min(metadata_value_id)
    FROM   metadatavalue
    WHERE dspace_object_id IN (SELECT uuid FROM bitstream)
    GROUP BY text_value, dspace_object_id
);