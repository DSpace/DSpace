--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- Dynamic Layout secondary indexes on FK / join / filter columns
--
-- PostgreSQL does not auto-create indexes on foreign-key columns (only on
-- primary-key / unique constraints), so the per-entity-type layout lookup that
-- walks tab -> rows -> cells -> boxes (plus the downstream field / nested-field
-- fetches and the tab/box entity-type filters) would otherwise fall back to
-- sequential scans on installations with non-trivial layout configurations.
--
-- The indexed columns were confirmed by capturing the SQL emitted for
-- GET /api/layout/tabs/search/findByEntityType on a real PostgreSQL environment:
-- the ROWS_AND_CONTENT_GRAPH entity-graph joins (tab -> row -> cell -> box), the
-- tab/box entity-type + custom_filter predicates, and the hottest downstream
-- per-box / per-field fetches (field.box_id and field2nested.field_id, the most
-- frequent filter observed).
--
-- dynamic_layout_tab is intentionally NOT indexed on (entity_id, custom_filter):
-- the existing UNIQUE (entity_id, shortname, custom_filter) constraint already
-- creates a btree index whose leading column is entity_id, so the entity-type
-- lookup is served by that index (verified with EXPLAIN: the tab query uses the
-- unique-constraint index for the entity_id + custom_filter predicate). A
-- dedicated (entity_id, custom_filter) index would be redundant.
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
