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

-- Remove NOT NULL restrictions from the checksum columns of most_recent_checksum
ALTER TABLE most_recent_checksum MODIFY expected_checksum null;
ALTER TABLE most_recent_checksum MODIFY current_checksum null;

------------------------------------------------------
-- New Column language language in EPerson
------------------------------------------------------

alter table eperson ADD language VARCHAR2(64);
update eperson set language = 'en';

-- totally unused column
alter table bundle drop column mets_bitstream_id;

-------------------------------------------------------------------------------
-- Necessary for Configurable Submission functionality:
-- Modification to workspaceitem table to support keeping track
-- of the last page reached within a step in the Configurable Submission Process
-------------------------------------------------------------------------------
ALTER TABLE workspaceitem ADD page_reached INTEGER;


-------------------------------------------------------------------------
-- Increase the mimetype field size to support larger types, such as the 
-- new Word 2007 mimetypes.
-------------------------------------------------------------------------
ALTER TABLE BitstreamFormatRegistry MODIFY (mimetype VARCHAR(256));


-------------------------------------------------------------------------
-- Tables to manage cache of item counts for communities and collections
-------------------------------------------------------------------------

CREATE TABLE collection_item_count (
	collection_id INTEGER PRIMARY KEY REFERENCES collection(collection_id),
	count INTEGER
);

CREATE TABLE community_item_count (
	community_id INTEGER PRIMARY KEY REFERENCES community(community_id),
	count INTEGER
);

------------------------------------------------------------------
-- Remove sequences and tables of the old browse system
------------------------------------------------------------------

DROP SEQUENCE itemsbyauthor_seq;
DROP SEQUENCE itemsbytitle_seq;
DROP SEQUENCE itemsbydate_seq;
DROP SEQUENCE itemsbydateaccessioned_seq;
DROP SEQUENCE itemsbysubject_seq;

DROP TABLE ItemsByAuthor CASCADE CONSTRAINTS;
DROP TABLE ItemsByTitle CASCADE CONSTRAINTS;
DROP TABLE ItemsByDate CASCADE CONSTRAINTS;
DROP TABLE ItemsByDateAccessioned CASCADE CONSTRAINTS;
DROP TABLE ItemsBySubject CASCADE CONSTRAINTS;

DROP TABLE History CASCADE CONSTRAINTS;
DROP TABLE HistoryState CASCADE CONSTRAINTS;

----------------------------------------------------------------
-- Add indexes for foreign key columns
----------------------------------------------------------------

CREATE INDEX fe_bitstream_fk_idx ON FileExtension(bitstream_format_id);

CREATE INDEX bit_bitstream_fk_idx ON Bitstream(bitstream_format_id);

CREATE INDEX g2g_parent_fk_idx ON Group2Group(parent_id);
CREATE INDEX g2g_child_fk_idx ON Group2Group(child_id);

-- CREATE INDEX g2gc_parent_fk_idx ON Group2Group(parent_id);
-- CREATE INDEX g2gc_child_fk_idx ON Group2Group(child_id);

CREATE INDEX item_submitter_fk_idx ON Item(submitter_id);

CREATE INDEX bundle_primary_fk_idx ON Bundle(primary_bitstream_id);

CREATE INDEX item2bundle_bundle_fk_idx ON Item2Bundle(bundle_id);

CREATE INDEX bundle2bits_bitstream_fk_idx ON Bundle2Bitstream(bitstream_id);

CREATE INDEX metadatavalue_field_fk_idx ON MetadataValue(metadata_field_id);

CREATE INDEX community_logo_fk_idx ON Community(logo_bitstream_id);

CREATE INDEX collection_logo_fk_idx ON Collection(logo_bitstream_id);
CREATE INDEX collection_template_fk_idx ON Collection(template_item_id);
CREATE INDEX collection_workflow1_fk_idx ON Collection(workflow_step_1);
CREATE INDEX collection_workflow2_fk_idx ON Collection(workflow_step_2);
CREATE INDEX collection_workflow3_fk_idx ON Collection(workflow_step_3);
CREATE INDEX collection_submitter_fk_idx ON Collection(submitter);
CREATE INDEX collection_admin_fk_idx ON Collection(admin);

CREATE INDEX com2com_parent_fk_idx ON Community2Community(parent_comm_id);
CREATE INDEX com2com_child_fk_idx ON Community2Community(child_comm_id);

CREATE INDEX rp_eperson_fk_idx ON ResourcePolicy(eperson_id);
CREATE INDEX rp_epersongroup_fk_idx ON ResourcePolicy(epersongroup_id);

CREATE INDEX epg2ep_eperson_fk_idx ON EPersonGroup2EPerson(eperson_id);

CREATE INDEX workspace_item_fk_idx ON WorkspaceItem(item_id);
CREATE INDEX workspace_coll_fk_idx ON WorkspaceItem(collection_id);

-- CREATE INDEX workflow_item_fk_idx ON WorkflowItem(item_id);
CREATE INDEX workflow_coll_fk_idx ON WorkflowItem(collection_id);
CREATE INDEX workflow_owner_fk_idx ON WorkflowItem(owner);

CREATE INDEX tasklist_eperson_fk_idx ON TasklistItem(eperson_id);
CREATE INDEX tasklist_workflow_fk_idx ON TasklistItem(workflow_id);

CREATE INDEX subs_eperson_fk_idx ON Subscription(eperson_id);
CREATE INDEX subs_collection_fk_idx ON Subscription(collection_id);

CREATE INDEX epg2wi_group_fk_idx ON epersongroup2workspaceitem(eperson_group_id);
CREATE INDEX epg2wi_workspace_fk_idx ON epersongroup2workspaceitem(workspace_item_id);

CREATE INDEX Comm2Item_community_fk_idx ON Communities2Item( community_id );

CREATE INDEX mrc_result_fk_idx ON most_recent_checksum( result );

CREATE INDEX ch_result_fk_idx ON checksum_history( result );

