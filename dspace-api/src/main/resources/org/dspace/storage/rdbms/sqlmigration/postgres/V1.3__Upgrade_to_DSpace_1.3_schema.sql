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

CREATE SEQUENCE epersongroup2workspaceitem_seq;

-------------------------------------------------------------------------------
-- create the new EPersonGroup2WorkspaceItem table
-------------------------------------------------------------------------------

CREATE TABLE epersongroup2workspaceitem 
(
  id integer DEFAULT nextval('epersongroup2workspaceitem_seq'),
  eperson_group_id integer REFERENCES EPersonGroup(eperson_group_id),
  workspace_item_id integer REFERENCES WorkspaceItem(workspace_item_id),
  CONSTRAINT epersongroup2item_pkey PRIMARY KEY (id)
);

-------------------------------------------------------------------------------
-- modification to collection table to support being able to change the
-- submitter and collection admin group names
-------------------------------------------------------------------------------
ALTER TABLE collection ADD submitter INTEGER REFERENCES EPersonGroup( eperson_group_id );

ALTER TABLE collection ADD admin INTEGER REFERENCES EPersonGroup( eperson_group_id );

ALTER TABLE eperson ADD netid varchar(64) UNIQUE;

UPDATE collection SET submitter=(SELECT eperson_group_id FROM epersongroup WHERE epersongroup.name = 'COLLECTION_' || collection_id || '_SUBMIT'); 

UPDATE collection SET admin=(SELECT eperson_group_id FROM epersongroup WHERE epersongroup.name = 'COLLECTION_' || collection_id  || '_ADMIN'); 

-------------------------------------------------------------------------------
-- Additional indices for performance
-------------------------------------------------------------------------------

-- index by resource id and resource type id
CREATE INDEX handle_resource_id_and_type_idx ON handle(resource_id, resource_type_id);

-- Indexing browse tables update/re-index performance
CREATE INDEX Communities2Item_item_id_idx ON Communities2Item( item_id );
CREATE INDEX ItemsByAuthor_item_id_idx ON ItemsByAuthor(item_id);
CREATE INDEX ItemsByTitle_item_id_idx ON ItemsByTitle(item_id);
CREATE INDEX ItemsByDate_item_id_idx ON ItemsByDate(item_id);
CREATE INDEX ItemsByDateAccessioned_item_id_idx ON ItemsByDateAccessioned(item_id);

-- Improve mapping tables
CREATE INDEX Community2Collection_community_id_idx ON Community2Collection(community_id);
CREATE INDEX Community2Collection_collection_id_idx ON Community2Collection(collection_id);
CREATE INDEX Collection2Item_item_id_idx ON Collection2Item( item_id );
