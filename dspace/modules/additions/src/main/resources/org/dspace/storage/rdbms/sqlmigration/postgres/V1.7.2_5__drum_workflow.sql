-------------------------------------------------------
-- Sequences for creating new IDs (primary keys) for
-- tables.  Each table must have a corresponding
-- sequence called 'tablename_seq'.
-------------------------------------------------------
CREATE SEQUENCE collection2workspaceitem_seq;
CREATE SEQUENCE collection2workflowitem_seq;


-------------------------------------------------------
-- Collection2WorkspaceItem table
-------------------------------------------------------
CREATE TABLE Collection2WorkspaceItem
(
  id                  INTEGER PRIMARY KEY,
  collection_id       INTEGER REFERENCES Collection(collection_id),
  workspace_item_id   INTEGER REFERENCES WorkspaceItem(workspace_item_id)
);

-- index by workspace_item_id
CREATE INDEX collection2workspaceitem_workspace_idx ON Collection2WorkspaceItem(workspace_item_id);

-------------------------------------------------------
-- Collection2WorkflowItem table
-------------------------------------------------------
CREATE TABLE Collection2WorkflowItem
(
  id            INTEGER PRIMARY KEY,
  collection_id INTEGER REFERENCES Collection(collection_id),
  workflow_id   INTEGER REFERENCES WorkflowItem(workflow_id)
);

-- index by workflow_id
CREATE INDEX collection2workflowitem_workflow_idx ON Collection2WorkflowItem(workflow_id);
