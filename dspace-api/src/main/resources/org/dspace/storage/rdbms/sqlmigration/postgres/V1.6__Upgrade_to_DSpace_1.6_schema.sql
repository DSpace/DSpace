--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- ===============================================================
-- WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
--
-- DO NOT MANUALLY RUN THIS DATABASE MIGRATION. IT WILL BE EXECUTED
-- AUTOMATICALLY (IF NEEDED) BY "FLYWAY" WHEN YOU STARTUP DSPACE.
-- http://flywaydb.org/
-- ===============================================================

------------------------------------------------------------------
-- New Column for Community Admin - Delegated Admin patch (DS-228)
------------------------------------------------------------------
ALTER TABLE community ADD admin INTEGER REFERENCES epersongroup ( eperson_group_id );
CREATE INDEX community_admin_fk_idx ON Community(admin);

-------------------------------------------------------------------------
-- DS-236 schema changes for Authority Control of Metadata Values
-------------------------------------------------------------------------
ALTER TABLE MetadataValue ADD  authority VARCHAR(100);
ALTER TABLE MetadataValue ADD confidence INTEGER DEFAULT -1;

------------------------------------------------------------------
-- New tables /sequences for the harvester functionality (DS-289)
------------------------------------------------------------------
CREATE SEQUENCE harvested_collection_seq;
CREATE SEQUENCE harvested_item_seq;

-------------------------------------------------------
-- Create the harvest settings table
-------------------------------------------------------
-- Values used by the OAIHarvester to harvest a collection
-- HarvestInstance is the DAO class for this table

CREATE TABLE harvested_collection
(
    collection_id INTEGER REFERENCES collection(collection_id) ON DELETE CASCADE,
    harvest_type INTEGER,
    oai_source VARCHAR,
    oai_set_id VARCHAR,
    harvest_message VARCHAR,
    metadata_config_id VARCHAR,
    harvest_status INTEGER,
    harvest_start_time TIMESTAMP WITH TIME ZONE,
    last_harvested TIMESTAMP WITH TIME ZONE,
    id INTEGER PRIMARY KEY
);

CREATE INDEX harvested_collection_fk_idx ON harvested_collection(collection_id);


CREATE TABLE harvested_item
(
    item_id INTEGER REFERENCES item(item_id) ON DELETE CASCADE,
    last_harvested TIMESTAMP WITH TIME ZONE,
    oai_id VARCHAR,
    id INTEGER PRIMARY KEY
);

CREATE INDEX harvested_item_fk_idx ON harvested_item(item_id);


-------------------------------------------------------------------------
-- DS-260 Cleanup of Owning collection column for template item created
-- with the JSPUI after the collection creation
-------------------------------------------------------------------------
UPDATE item SET owning_collection = null WHERE item_id IN
        (SELECT template_item_id FROM collection WHERE template_item_id IS NOT null);

-- Recreate restraints with a know name and deferrable option!
-- (The previous version of these constraints is dropped by org.dspace.storage.rdbms.migration.V1_5_9__Drop_constraint_for_DSpace_1_6_schema)
ALTER TABLE community2collection ADD CONSTRAINT comm2coll_collection_fk FOREIGN KEY (collection_id) REFERENCES collection DEFERRABLE;
ALTER TABLE community2community ADD CONSTRAINT com2com_child_fk FOREIGN KEY (child_comm_id) REFERENCES community DEFERRABLE;
ALTER TABLE collection2item ADD CONSTRAINT coll2item_item_fk FOREIGN KEY (item_id) REFERENCES item DEFERRABLE;


--------------------------------------------------------------------------
-- DS-295 CC License being assigned incorrect Mime Type during submission.
--------------------------------------------------------------------------
UPDATE bitstream SET bitstream_format_id =
   (SELECT bitstream_format_id FROM bitstreamformatregistry WHERE short_description = 'CC License')
   WHERE name = 'license_text' AND source = 'org.dspace.license.CreativeCommons';

UPDATE bitstream SET bitstream_format_id =
   (SELECT bitstream_format_id FROM bitstreamformatregistry WHERE short_description = 'RDF XML')
   WHERE name = 'license_rdf' AND source = 'org.dspace.license.CreativeCommons';
