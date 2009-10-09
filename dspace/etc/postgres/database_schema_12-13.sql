--
-- database_schema_12-13.sql
--
-- Version: $Revision$
--
-- Date:    $Date$
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
-- SQL commands to upgrade the database schema of a live DSpace 1.2 or 1.2.x
-- to the DSpace 1.3 database schema
-- 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 

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
