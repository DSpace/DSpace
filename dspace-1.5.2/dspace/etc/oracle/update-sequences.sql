--
-- update-sequences.sql
--
-- Version: $Revision$
--
-- Date:    $Date$
--
-- Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
-- Institute of Technology.  All rights reserved.
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
-- - Neither the name of the Hewlett-Packard Company nor the name of the
-- Massachusetts Institute of Technology nor the names of their
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

-- SQL code to update the ID (primary key) generating sequences, if some
-- import operation has set explicit IDs.
--
-- Sequences are used to generate IDs for new rows in the database.  If a
-- bulk import operation, such as an SQL dump, specifies primary keys for
-- imported data explicitly, the sequences are out of sync and need updating.
-- This SQL code does just that.
--
-- This should rarely be needed; any bulk import should be performed using the
-- org.dspace.content API which is safe to use concurrently and in multiple
-- JVMs.  The SQL code below will typically only be required after a direct
-- SQL data dump from a backup or somesuch.
 

-- There should be one of these calls for every ID sequence defined in
-- database_schema.sql.

-- depends on being run from sqlplus with incseq.sql in the current path
-- you can find incseq.sql at: http://akadia.com/services/scripts/incseq.sql

@updateseq.sql bitstreamformatregistry_seq bitstreamformatregistry bitstream_format_id
@updateseq.sql fileextension_seq fileextension file_extension_id
@updateseq.sql bitstream_seq bitstream bitstream_id
@updateseq.sql eperson_seq eperson eperson_id
@updateseq.sql epersongroup_seq epersongroup eperson_group_id
@updateseq.sql group2group_seq group2group id
@updateseq.sql group2groupcache group2groupcache id
@updateseq.sql item_seq item item_id
@updateseq.sql bundle_seq bundle bundle_id
@updateseq.sql item2bundle_seq item2bundle id
@updateseq.sql bundle2bitstream_seq bundle2bitstream id
@updateseq.sql dctyperegistry_seq dctyperegistry dc_type_id
@updateseq.sql dcvalue_seq dcvalue dc_value_id
@updateseq.sql community_seq community community_id
@updateseq.sql community2community_seq community2community id
@updateseq.sql collection_seq collection collection_id
@updateseq.sql community2collection_seq community2collection id
@updateseq.sql collection2item_seq collection2item id
@updateseq.sql resourcepolicy_seq resourcepolicy policy_id
@updateseq.sql epersongroup2eperson_seq epersongroup2eperson id
@updateseq.sql handle_seq handle handle_id
@updateseq.sql workspaceitem_seq workspaceitem workspace_item_id
@updateseq.sql workflowitem_seq workflowitem workflow_id
@updateseq.sql tasklistitem_seq tasklistitem tasklist_id
@updateseq.sql registrationdata_seq registrationdata registrationdata_id
@updateseq.sql subscription_seq subscription subscription_id
@updateseq.sql history_seq history history_id
@updateseq.sql historystate_seq historystate history_state_id
@updateseq.sql communities2item_seq communities2item id
@updateseq.sql itemsbyauthor_seq itemsbyauthor items_by_author_id
@updateseq.sql itemsbytitle_seq itemsbytitle items_by_title_id
@updateseq.sql itemsbydate_seq itemsbydate items_by_date_id
@updateseq.sql itemsbydateaccessioned_seq itemsbydateaccessioned items_by_date_accessioned_id
@updateseq.sql epersongroup2workspaceitem_seq epersongroup2workspaceitem id