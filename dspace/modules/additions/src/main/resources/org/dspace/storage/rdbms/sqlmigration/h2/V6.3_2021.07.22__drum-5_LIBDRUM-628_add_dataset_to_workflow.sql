------------------------------------------------------
-- LIBDRUM-628 - Add dataset to workflowitem, workflowitem, workflowitem
------------------------------------------------------
ALTER TABLE workspaceitem ADD COLUMN is_dataset BOOL;
ALTER TABLE workflowitem ADD COLUMN is_dataset BOOL;

UPDATE workspaceitem SET is_dataset=false;
UPDATE workflowitem SET is_dataset=false;

-- Commenting out the following because not sure how to convert to H2, and
-- it seems unnecessary, because the test database should have no data.
-- DO $$
--     BEGIN
--         IF EXISTS
--             ( SELECT 1
--               FROM   information_schema.tables
--               WHERE  table_name = 'cwf_workflowitem'
--             )
--         THEN
--             UPDATE recipes
--             SET lock = NULL
--             WHERE lock IS NOT NULL ;
--         END IF ;
--     END
--    $$ ;