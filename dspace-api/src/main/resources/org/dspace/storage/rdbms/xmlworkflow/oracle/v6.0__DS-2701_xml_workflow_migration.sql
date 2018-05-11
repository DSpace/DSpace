--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

----------------------------------------------------
-- Database Schema Update for XML/Configurable Workflow (for DSpace 6.0)
--
-- This file will automatically create/update your
-- DSpace Database tables to support XML/Configurable workflows.
-- However, it does NOT migrate your existing classic
-- workflows. That step is performed by the corresponding
-- "data_workflow_migration.sql" script.
--
-- This script is called automatically by the following
-- Flyway Java migration class:
-- org.dspace.storage.rdbms.xmlworkflow.V6_0_2015_09_01__DS_2701_Enable_XMLWorkflow_Migration
----------------------------------------------------

CREATE SEQUENCE cwf_workflowitem_seq;
CREATE SEQUENCE cwf_collectionrole_seq;
CREATE SEQUENCE cwf_workflowitemrole_seq;
CREATE SEQUENCE cwf_claimtask_seq;
CREATE SEQUENCE cwf_in_progress_user_seq;
CREATE SEQUENCE cwf_pooltask_seq;


CREATE TABLE cwf_workflowitem
(
  workflowitem_id INTEGER PRIMARY KEY,
  item_id        RAW(16) REFERENCES item(uuid) UNIQUE,
  collection_id  RAW(16) REFERENCES collection(uuid),
  --
  -- Answers to questions on first page of submit UI
  multiple_titles       NUMBER(1),
  published_before      NUMBER(1),
  multiple_files        NUMBER(1)
  -- Note: stage reached not applicable here - people involved in workflow
  -- can always jump around submission UI
);


CREATE INDEX cwf_workflowitem_coll_fk_idx ON cwf_workflowitem(collection_id);


CREATE TABLE cwf_collectionrole (
collectionrole_id INTEGER PRIMARY KEY,
role_id VARCHAR2(256),
collection_id RAW(16) REFERENCES collection(uuid),
group_id RAW(16) REFERENCES epersongroup(uuid)
);
ALTER TABLE cwf_collectionrole
ADD CONSTRAINT cwf_collectionrole_unique UNIQUE (role_id, collection_id, group_id);

CREATE INDEX cwf_cr_coll_role_fk_idx ON cwf_collectionrole(collection_id,role_id);
CREATE INDEX cwf_cr_coll_fk_idx ON cwf_collectionrole(collection_id);


CREATE TABLE cwf_workflowitemrole (
  workflowitemrole_id INTEGER PRIMARY KEY,
  role_id VARCHAR2(256),
  workflowitem_id integer REFERENCES cwf_workflowitem(workflowitem_id),
  eperson_id RAW(16) REFERENCES eperson(uuid),
  group_id RAW(16) REFERENCES epersongroup(uuid)
);
ALTER TABLE cwf_workflowitemrole
ADD CONSTRAINT cwf_workflowitemrole_unique UNIQUE (role_id, workflowitem_id, eperson_id, group_id);

CREATE INDEX cwf_wfir_item_role_fk_idx ON cwf_workflowitemrole(workflowitem_id,role_id);
CREATE INDEX cwf_wfir_item_fk_idx ON cwf_workflowitemrole(workflowitem_id);


CREATE TABLE cwf_pooltask (
  pooltask_id   INTEGER PRIMARY KEY,
  workflowitem_id   INTEGER REFERENCES cwf_workflowitem(workflowitem_id),
  workflow_id   VARCHAR2(256),
  step_id       VARCHAR2(256),
  action_id     VARCHAR2(256),
  eperson_id    RAW(16) REFERENCES EPerson(uuid),
  group_id      RAW(16) REFERENCES epersongroup(uuid)
);

CREATE INDEX cwf_pt_eperson_fk_idx ON cwf_pooltask(eperson_id);
CREATE INDEX cwf_pt_workflow_fk_idx ON cwf_pooltask(workflowitem_id);
CREATE INDEX cwf_pt_workflow_eperson_fk_idx ON cwf_pooltask(eperson_id,workflowitem_id);



CREATE TABLE cwf_claimtask (
  claimtask_id INTEGER PRIMARY KEY,
  workflowitem_id integer REFERENCES cwf_workflowitem(workflowitem_id),
  workflow_id VARCHAR2(256),
  step_id VARCHAR2(256),
  action_id VARCHAR2(256),
  owner_id RAW(16) REFERENCES eperson(uuid)
);

ALTER TABLE cwf_claimtask
ADD CONSTRAINT cwf_claimtask_unique UNIQUE (step_id, workflowitem_id, workflow_id, owner_id, action_id);

CREATE INDEX cwf_ct_workflow_fk_idx ON cwf_claimtask(workflowitem_id);
CREATE INDEX cwf_ct_workflow_eperson_fk_idx ON cwf_claimtask(workflowitem_id,owner_id);
CREATE INDEX cwf_ct_eperson_fk_idx ON cwf_claimtask(owner_id);
CREATE INDEX cwf_ct_wfs_fk_idx ON cwf_claimtask(workflowitem_id,step_id);
CREATE INDEX cwf_ct_wfs_action_fk_idx ON cwf_claimtask(workflowitem_id,step_id,action_id);
CREATE INDEX cwf_ct_wfs_action_e_fk_idx ON cwf_claimtask(workflowitem_id,step_id,action_id,owner_id);


CREATE TABLE cwf_in_progress_user (
  in_progress_user_id INTEGER PRIMARY KEY,
  workflowitem_id integer REFERENCES cwf_workflowitem(workflowitem_id),
  user_id RAW(16) REFERENCES eperson(uuid),
  finished NUMBER(1) DEFAULT  0
);

ALTER TABLE cwf_in_progress_user
ADD CONSTRAINT cwf_in_progress_user_unique UNIQUE (workflowitem_id, user_id);

CREATE INDEX cwf_ipu_workflow_fk_idx ON cwf_in_progress_user(workflowitem_id);
CREATE INDEX cwf_ipu_eperson_fk_idx ON cwf_in_progress_user(user_id);

