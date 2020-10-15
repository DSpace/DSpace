--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- add the extra configuration to the harvested collection table
-- DO is not supporter pre version 9.0 in postgres. Do stuff
-- the old way
CREATE OR REPLACE FUNCTION add_cristin_columns() RETURNS void AS
$$
BEGIN
  BEGIN
    ALTER TABLE harvested_collection ADD COLUMN metadata_authority_type VARCHAR;
    EXCEPTION
    WHEN DUPLICATE_COLUMN THEN RAISE NOTICE 'column metadata_authority_type already exists in harvested_collection.';
  END;
  BEGIN
    ALTER TABLE harvested_collection ADD COLUMN bundle_versioning_strategy VARCHAR;
    EXCEPTION
    WHEN DUPLICATE_COLUMN THEN RAISE NOTICE 'column bundle_versioning_strategy already exists in harvested_collection.';
  END;
  BEGIN
    ALTER TABLE harvested_collection ADD COLUMN workflow_process VARCHAR;
    EXCEPTION
    WHEN DUPLICATE_COLUMN THEN RAISE NOTICE 'column workflow_process already exists in harvested_collection.';
  END;
  BEGIN
    ALTER TABLE harvested_collection ADD COLUMN ingest_filter VARCHAR;
    EXCEPTION
    WHEN DUPLICATE_COLUMN THEN RAISE NOTICE 'column ingest_filter already exists in harvested_collection.';
  END;
END;
$$ LANGUAGE plpgsql VOLATILE;

SELECT add_cristin_columns();

DROP FUNCTION add_cristin_columns();