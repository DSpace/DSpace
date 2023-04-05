--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

---------------------------------------------------------------
-- DS-3410
---------------------------------------------------------------
-- This script will create lost indexes 
---------------------------------------------------------------

CREATE INDEX resourcepolicy_object on resourcepolicy(dspace_object);
CREATE INDEX metadatavalue_object on metadatavalue(dspace_object_id);
CREATE INDEX metadatavalue_field_object on metadatavalue(metadata_field_id, dspace_object_id);