--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- Dynamic Layout Metric 2 Box - squashed idempotent migration
-- Consolidates: V7.0_2020.12.08 (metric2box)
--
-- Handles three states idempotently (legacy cris_* present / dynamic_* already
-- present / fresh install). The legacy foreign-key column cris_layout_box_id is
-- mapped to the new dynamic_layout_box_id column during the copy.
-------------------------------------------------------

-- Step 1: create the new table (safe on any database state)
CREATE TABLE IF NOT EXISTS dynamic_layout_metric2box
(
    metric_type          VARCHAR(255) NOT NULL,
    dynamic_layout_box_id INTEGER NOT NULL,
    position             INTEGER NOT NULL,
    CONSTRAINT dynamic_layout_metric2box_pkey PRIMARY KEY (dynamic_layout_box_id, metric_type),
    CONSTRAINT dynamic_layout_box2metric_box_id_fkey FOREIGN KEY (dynamic_layout_box_id)
        REFERENCES dynamic_layout_box (id)
);

-- Step 2: migrate data from the legacy cris_layout_metric2box table when present.
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_tables
               WHERE schemaname = 'public' AND tablename = 'cris_layout_metric2box') THEN
        INSERT INTO dynamic_layout_metric2box (metric_type, dynamic_layout_box_id, position)
        SELECT metric_type, cris_layout_box_id, position FROM cris_layout_metric2box;

        DROP TABLE cris_layout_metric2box CASCADE;
    END IF;
END $$;
