CREATE SEQUENCE cwf_workflowitem_seq;
CREATE SEQUENCE cwf_collectionrole_seq;
CREATE SEQUENCE cwf_workflowitemrole_seq;
CREATE SEQUENCE cwf_claimtask_seq;
CREATE SEQUENCE cwf_in_progress_user_seq;
CREATE SEQUENCE cwf_pooltask_seq;


CREATE TABLE cwf_workflowitem
(
  workflowitem_id INTEGER PRIMARY KEY,
  item_id        INTEGER REFERENCES item(item_id) UNIQUE,
  collection_id  INTEGER REFERENCES collection(collection_id),
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
collection_id integer REFERENCES collection(collection_id),
group_id integer REFERENCES epersongroup(eperson_group_id)
);
ALTER TABLE cwf_collectionrole
ADD CONSTRAINT cwf_collectionrole_unique UNIQUE (role_id, collection_id, group_id);

CREATE INDEX cwf_cr_coll_role_fk_idx ON cwf_collectionrole(collection_id,role_id);
CREATE INDEX cwf_cr_coll_fk_idx ON cwf_collectionrole(collection_id);


CREATE TABLE cwf_workflowitemrole (
  workflowitemrole_id INTEGER PRIMARY KEY,
  role_id VARCHAR2(256),
  workflowitem_id integer REFERENCES cwf_workflowitem(workflowitem_id),
  eperson_id integer REFERENCES eperson(eperson_id),
  group_id integer REFERENCES epersongroup(eperson_group_id)
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
  eperson_id    INTEGER REFERENCES EPerson(eperson_id),
  group_id      INTEGER REFERENCES epersongroup(eperson_group_id)
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
  owner_id integer REFERENCES eperson(eperson_id)
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
  user_id integer REFERENCES eperson(eperson_id),
  finished NUMBER(1) DEFAULT  0
);

ALTER TABLE cwf_in_progress_user
ADD CONSTRAINT cwf_in_progress_user_unique UNIQUE (workflowitem_id, user_id);

CREATE INDEX cwf_ipu_workflow_fk_idx ON cwf_in_progress_user(workflowitem_id);
CREATE INDEX cwf_ipu_eperson_fk_idx ON cwf_in_progress_user(user_id);

