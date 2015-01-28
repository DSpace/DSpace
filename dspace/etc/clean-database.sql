--
-- clean-database.sql
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
--  DSpace database cleaner
--
--    This SQL "cleans" a database used by a DSpace installation.  It removes
--    all tables etc. so the database is completely empty and ready for a
--    fresh installation.  Of course, this means all data is lost.
--
--    This should be kept in sync if database_schema.sql is updated.


-- Drop the views
DROP VIEW Community2Item;
DROP VIEW DCValue;

-- Then the tables
-- WARNING: The ordering below MATTERS as many tables have interdependencies
DROP TABLE collection_item_count;
DROP TABLE community_item_count;
DROP TABLE checksum_history;
DROP TABLE most_recent_checksum;
DROP TABLE checksum_results;
DROP TABLE Communities2Item;
DROP TABLE EPersonGroup2WorkspaceItem;
DROP TABLE Subscription;
DROP TABLE RegistrationData;
DROP TABLE TasklistItem;
DROP TABLE WorkflowItem;
DROP TABLE WorkspaceItem;
DROP TABLE Handle;
DROP TABLE Doi;
DROP TABLE EPersonGroup2EPerson;
DROP TABLE ResourcePolicy;
DROP TABLE Collection2Item;
DROP TABLE Community2Collection;
DROP TABLE Community2Community;
DROP TABLE MetadataValue;
DROP TABLE MetadataFieldRegistry;
DROP TABLE MetadataSchemaRegistry;
DROP TABLE Bundle2Bitstream;
DROP TABLE Item2Bundle;
DROP TABLE harvested_collection;
DROP TABLE harvested_item;
DROP TABLE Group2GroupCache;
DROP TABLE Group2Group;
DROP TABLE FileExtension;
DROP TABLE webapp;
DROP TABLE requestitem;
-- Drop main object tables near end as many other tables have dependencies on them
DROP TABLE versionitem;
DROP TABLE versionhistory;
DROP TABLE Community;
DROP TABLE Collection;
DROP TABLE Item;
DROP TABLE Bundle;
DROP TABLE Bitstream;
-- BitstreamFormatRegistry is referenced by bitstreams
DROP TABLE BitstreamFormatRegistry;
-- People and Groups are referenced by Items, Collections, Communities
DROP TABLE EPersonGroup;
DROP TABLE EPerson;


-- Now drop the sequences for ID (primary key) creation
DROP SEQUENCE bitstreamformatregistry_seq;
DROP SEQUENCE fileextension_seq;
DROP SEQUENCE bitstream_seq;
DROP SEQUENCE checksum_history_seq;
DROP SEQUENCE eperson_seq;
DROP SEQUENCE epersongroup_seq;
DROP SEQUENCE item_seq;
DROP SEQUENCE bundle_seq;
DROP SEQUENCE item2bundle_seq;
DROP SEQUENCE bundle2bitstream_seq;
DROP SEQUENCE dcvalue_seq;
DROP SEQUENCE community_seq;
DROP SEQUENCE community2community_seq;
DROP SEQUENCE collection_seq;
DROP SEQUENCE community2collection_seq;
DROP SEQUENCE collection2item_seq;
DROP SEQUENCE resourcepolicy_seq;
DROP SEQUENCE epersongroup2eperson_seq;
DROP SEQUENCE handle_seq;
DROP SEQUENCE doi_seq;
DROP SEQUENCE workspaceitem_seq;
DROP SEQUENCE workflowitem_seq;
DROP SEQUENCE tasklistitem_seq;
DROP SEQUENCE registrationdata_seq;
DROP SEQUENCE subscription_seq;
DROP SEQUENCE communities2item_seq;
DROP SEQUENCE epersongroup2workspaceitem_seq;
DROP SEQUENCE metadataschemaregistry_seq;
DROP SEQUENCE metadatafieldregistry_seq;
DROP SEQUENCE metadatavalue_seq;
DROP SEQUENCE group2group_seq;
DROP SEQUENCE group2groupcache_seq;
DROP SEQUENCE harvested_collection_seq;
DROP SEQUENCE harvested_item_seq;
DROP SEQUENCE versionhistory_seq;
DROP SEQUENCE versionitem_seq;
DROP SEQUENCE webapp_seq;
DROP SEQUENCE requestitem_seq;

-- Drop the getnextid() function
DROP FUNCTION getnextid;
