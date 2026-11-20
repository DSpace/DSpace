--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- Dynamic Layout secondary indexes on FK / join / filter columns (H2)
--
-- Mirrors the Postgres index migration so the H2-backed integration test suite
-- exercises the same schema shape. These indexes cover the columns the runtime
-- dynamic-layout query filters and joins on: the ROWS_AND_CONTENT_GRAPH
-- entity-graph joins (tab -> rows -> cells -> boxes) plus the downstream field /
-- nested-field fetches and the tab/box entity-type filters.
--
-- dynamic_layout_tab is intentionally NOT indexed on (entity_id, custom_filter):
-- the existing UNIQUE (entity_id, shortname, custom_filter) constraint index has
-- entity_id as its leading column, so the entity-type lookup is already served by
-- an index. A dedicated tab index would be redundant, matching the Postgres
-- migration.
-------------------------------------------------------

-- Row: row -> tab FK join.
CREATE INDEX IF NOT EXISTS idx_dynamic_layout_row_tab
    ON dynamic_layout_row (tab);

-- Cell: cell -> row FK join.
CREATE INDEX IF NOT EXISTS idx_dynamic_layout_cell_row
    ON dynamic_layout_cell (row);

-- Box: join from cell to boxes, plus the box DAO's own entity-type filter.
CREATE INDEX IF NOT EXISTS idx_dynamic_layout_box_cell
    ON dynamic_layout_box (cell);
CREATE INDEX IF NOT EXISTS idx_dynamic_layout_box_entity_id
    ON dynamic_layout_box (entity_id);

-- Field: downstream field fetch per box (where box_id = ?).
CREATE INDEX IF NOT EXISTS idx_dynamic_layout_field_box_id
    ON dynamic_layout_field (box_id);

-- Field2nested: downstream nested-field fetch per field (where field_id = ?),
-- the most frequently executed filter in the captured runtime session.
CREATE INDEX IF NOT EXISTS idx_dynamic_layout_field2nested_field_id
    ON dynamic_layout_field2nested (field_id);
