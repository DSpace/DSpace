--
-- database_schema_15-16.sql
--
-- Version: $Revision$
--
-- Date:    $Date: 2009-04-23 22:26:59 -0500 (Thu, 23 Apr 2009) $
--
-- Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
--
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are
-- met:
--
-- - Redistributions of source code must retain the above copyright
-- notice, this list of conditions and the following disclaimer.
--
-- - Redistributions in binary form must reproduce the above copyright
-- notice, this list of conditions and the following disclaimer in the
-- documentation and/or other materials provided with the distribution.
--
-- - Neither the name of the DSpace Foundation nor the names of its
-- contributors may be used to endorse or promote products derived from
-- this software without specific prior written permission.
--
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
-- ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
-- LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
-- A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
-- HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
-- INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
-- BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
-- OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
-- ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
-- TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
-- USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
-- DAMAGE.

--
-- SQL commands to upgrade the database schema of a live DSpace 1.5 or 1.5.x
-- to the DSpace 1.6 database schema
--
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.

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

ALTER TABLE community2collection DROP CONSTRAINT community2collection_collection_id_fkey;
ALTER TABLE community2collection ADD CONSTRAINT comm2coll_collection_fk FOREIGN KEY (collection_id) REFERENCES collection DEFERRABLE;

ALTER TABLE community2community DROP CONSTRAINT community2community_child_comm_id_fkey;
ALTER TABLE community2community ADD CONSTRAINT com2com_child_fk FOREIGN KEY (child_comm_id) REFERENCES community DEFERRABLE;

ALTER TABLE collection2item DROP CONSTRAINT collection2item_item_id_fkey;
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
