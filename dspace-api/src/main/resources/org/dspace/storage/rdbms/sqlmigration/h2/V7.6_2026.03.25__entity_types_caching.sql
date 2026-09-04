--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- H2 does not support upper and hence the migration differs slightly from the postgres version
-- In a test environment this will not make a meaningful impact
CREATE INDEX entity_type_label_upper_idx ON entity_type (label);
