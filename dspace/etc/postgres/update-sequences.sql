--
-- update-sequences.sql
--
-- Version: $Revision: 4427 $
--
-- Date:    $Date: 2009-10-09 17:42:19 -0500 (Fri, 09 Oct 2009) $
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
-- Neither the name of the DSpace Foundation nor the names of its
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

SELECT SETVAL('bi_2_dis_seq', COALESCE(MAX(bi_2_dis_id), 1) ) FROM bi_2_dis;
SELECT SETVAL('bi_2_dmap_seq', COALESCE(MAX(map_id), 1) ) FROM bi_2_dmap;
SELECT SETVAL('bi_item_seq', COALESCE(MAX(id), 1) ) FROM bi_item;
SELECT SETVAL('bi_withdrawn_seq', COALESCE(MAX(id), 1) ) FROM bi_withdrawn;
SELECT SETVAL('bitstream_seq', COALESCE(MAX(bitstream_id), 1) ) FROM bitstream;
SELECT SETVAL('bitstreamformatregistry_seq', COALESCE(MAX(bitstream_format_id), 1) ) FROM bitstreamformatregistry;
SELECT SETVAL('bundle2bitstream_seq', COALESCE(MAX(id), 1) ) FROM bundle2bitstream;
SELECT SETVAL('bundle_seq', COALESCE(MAX(bundle_id), 1) ) FROM bundle;
SELECT SETVAL('checksum_history_check_id_seq', COALESCE(MAX(checksum_history_check_id_id), 1) ) FROM checksum_history_check_id;
SELECT SETVAL('collection2item_seq', COALESCE(MAX(id), 1) ) FROM collection2item;
SELECT SETVAL('collection_seq', COALESCE(MAX(collection_id), 1) ) FROM collection;
SELECT SETVAL('collectionrole_seq', COALESCE(MAX(collectionrole_id), 1) ) FROM collectionrole;
SELECT SETVAL('communities2item_seq', COALESCE(MAX(id), 1) ) FROM communities2item;
SELECT SETVAL('community2collection_seq', COALESCE(MAX(id), 1) ) FROM community2collection;
SELECT SETVAL('community2community_seq', COALESCE(MAX(id), 1) ) FROM community2community;
SELECT SETVAL('community_seq', COALESCE(MAX(community_id), 1) ) FROM community;
SELECT SETVAL('concept2concept_seq', COALESCE(MAX(id), 1) ) FROM concept2concept;
SELECT SETVAL('concept2conceptrole_seq', COALESCE(MAX(id), 1) ) FROM concept2conceptrole;
SELECT SETVAL('concept2term_seq', COALESCE(MAX(id), 1) ) FROM concept2term;
SELECT SETVAL('concept2termrole_seq', COALESCE(MAX(id), 1) ) FROM concept2termrole;
SELECT SETVAL('concept_seq', COALESCE(MAX(id), 1) ) FROM concept;
SELECT SETVAL('conceptmetadatavalue_seq', COALESCE(MAX(id), 1) ) FROM conceptmetadatavalue;
SELECT SETVAL('dcvalue_seq', COALESCE(MAX(dc_value_id), 1) ) FROM dcvalue;
SELECT SETVAL('doi_seq', COALESCE(MAX(doi_id), 1) ) FROM doi;
SELECT SETVAL('eperson_seq', COALESCE(MAX(eperson_id), 1) ) FROM eperson;
SELECT SETVAL('epersongroup2eperson_seq', COALESCE(MAX(id), 1) ) FROM epersongroup2eperson;
SELECT SETVAL('epersongroup2workspaceitem_seq', COALESCE(MAX(id), 1) ) FROM epersongroup2workspaceitem;
SELECT SETVAL('epersongroup_seq', COALESCE(MAX(eperson_group_id), 1) ) FROM epersongroup;
SELECT SETVAL('fileextension_seq', COALESCE(MAX(file_extension_id), 1) ) FROM fileextension;
SELECT SETVAL('group2group_seq', COALESCE(MAX(id), 1) ) FROM group2group;
SELECT SETVAL('group2groupcache_seq', COALESCE(MAX(id), 1) ) FROM group2groupcache;
SELECT SETVAL('handle_seq', COALESCE(MAX(handle_id), 1) ) FROM handle;
SELECT SETVAL('harvested_collection_seq', COALESCE(MAX(collection_id), 1) ) FROM harvested_collection;
SELECT SETVAL('harvested_item_seq', COALESCE(MAX(item_id), 1) ) FROM harvested_item;
SELECT SETVAL('item2bundle_seq', COALESCE(MAX(id), 1) ) FROM item2bundle;
SELECT SETVAL('item_seq', COALESCE(MAX(item_id), 1) ) FROM item;
SELECT SETVAL('manuscript_seq', COALESCE(MAX(manuscript_id), 1) ) FROM manuscript;
SELECT SETVAL('metadatafieldregistry_seq', COALESCE(MAX(metadata_field_id), 1) ) FROM metadatafieldregistry;
SELECT SETVAL('metadataschemaregistry_seq', COALESCE(MAX(metadata_schema_id), 1) ) FROM metadataschemaregistry;
SELECT SETVAL('metadatavalue_seq', COALESCE(MAX(metadata_value_id), 1) ) FROM metadatavalue;
SELECT SETVAL('oauth_token_seq', COALESCE(MAX(oauth_token_id), 1) ) FROM oauth_token;
SELECT SETVAL('organization_seq', COALESCE(MAX(organization_id), 1) ) FROM organization;
SELECT SETVAL('registrationdata_seq', COALESCE(MAX(registrationdata_id), 1) ) FROM registrationdata;
SELECT SETVAL('resourcepolicy_seq', COALESCE(MAX(policy_id), 1) ) FROM resourcepolicy;
SELECT SETVAL('rest_resource_authz_seq', COALESCE(MAX(rest_resource_authz_id), 1) ) FROM rest_resource_authz;
SELECT SETVAL('scheme2concept_seq', COALESCE(MAX(id), 1) ) FROM scheme2concept;
SELECT SETVAL('schememetadatavalue_seq', COALESCE(MAX(id), 1) ) FROM schememetadatavalue;
SELECT SETVAL('shoppingcart_seq', COALESCE(MAX(cart_id), 1) ) FROM shoppingcart;
SELECT SETVAL('subscription_seq', COALESCE(MAX(subscription_id), 1) ) FROM subscription;
SELECT SETVAL('tasklistitem_seq', COALESCE(MAX(tasklist_id), 1) ) FROM tasklistitem;
SELECT SETVAL('taskowner_seq', COALESCE(MAX(taskowner_id), 1) ) FROM taskowner;
SELECT SETVAL('term_seq', COALESCE(MAX(id), 1) ) FROM term;
SELECT SETVAL('termmetadatavalue_seq', COALESCE(MAX(id), 1) ) FROM termmetadatavalue;
SELECT SETVAL('versionhistory_seq', COALESCE(MAX(versionhistory_id), 1) ) FROM versionhistory;
SELECT SETVAL('versionitem_seq', COALESCE(MAX(versionitem_id), 1) ) FROM versionitem;
SELECT SETVAL('voucher_seq', COALESCE(MAX(voucher_id), 1) ) FROM voucher;
SELECT SETVAL('workflowitem_seq', COALESCE(MAX(workflow_id), 1) ) FROM workflowitem;
SELECT SETVAL('workflowitemrole_seq', COALESCE(MAX(workflowitemrole_id), 1) ) FROM workflowitemrole;
SELECT SETVAL('workspaceitem_seq', COALESCE(MAX(workspace_item_id), 1) ) FROM workspaceitem;
