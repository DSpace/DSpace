--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

----------------------------------------------------
-- Make sure the metadatavalue.place column starts at 0 instead of 1
----------------------------------------------------

CREATE LOCAL TEMPORARY TABLE mdv_minplace (
  dspace_object_id UUID NOT NULL,
  metadata_field_id INT NOT NULL,
  minplace INT NOT NULL
);

INSERT INTO mdv_minplace
SELECT dspace_object_id, metadata_field_id, MIN(place) AS minplace
FROM metadatavalue
GROUP BY dspace_object_id, metadata_field_id;

UPDATE metadatavalue AS mdv
SET place = mdv.place - (
  SELECT minplace FROM mdv_minplace AS mp
  WHERE mp.dspace_object_id = mdv.dspace_object_id
    AND mp.metadata_field_id = mdv.metadata_field_id
);
