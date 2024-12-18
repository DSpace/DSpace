--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- In the workspaceitem table, if there are multiple rows referring to the same item ID, keep only the first of them.
WITH dedup AS (
    SELECT item_id, MIN(workspace_item_id) AS workspace_item_id
    FROM workspaceitem
    GROUP BY item_id
    HAVING COUNT(workspace_item_id) > 1
)
DELETE FROM workspaceitem
USING dedup
WHERE workspaceitem.item_id = dedup.item_id AND workspaceitem.workspace_item_id <> dedup.workspace_item_id;

-- Enforce uniqueness of item_id in workspaceitem table.
ALTER TABLE workspaceitem ADD CONSTRAINT unique_item_id UNIQUE(item_id);
