--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- In the workspaceitem table, if there are multiple rows referring to the same item ID, keep only the first of them.
DELETE FROM workspaceitem WHERE EXISTS (
    SELECT item_id
    FROM workspaceitem
    GROUP BY item_id
    HAVING COUNT(workspace_item_id) > 1
) AND workspaceitem.workspace_item_id NOT IN (
    SELECT MIN(workspace_item_id) AS workspace_item_id
    FROM workspaceitem
    GROUP BY item_id
);
-- Identify which rows have duplicates, and compute their replacements.
ALTER TABLE workspaceitem ADD CONSTRAINT unique_item_id UNIQUE(item_id);
