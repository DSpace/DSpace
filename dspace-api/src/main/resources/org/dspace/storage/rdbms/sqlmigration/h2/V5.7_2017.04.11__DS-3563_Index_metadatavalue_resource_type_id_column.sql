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

DROP INDEX IF EXISTS metadatavalue_resource_type_id_idx;

CREATE INDEX metadatavalue_resource_type_id_idx ON metadatavalue (resource_type_id);
