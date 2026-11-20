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
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS dynamic_layout_metric2box
(
    metric_type          VARCHAR(255) NOT NULL,
    dynamic_layout_box_id INTEGER NOT NULL,
    position             INTEGER NOT NULL,
    CONSTRAINT dynamic_layout_metric2box_pkey PRIMARY KEY (dynamic_layout_box_id, metric_type),
    CONSTRAINT dynamic_layout_box2metric_box_id_fkey FOREIGN KEY (dynamic_layout_box_id)
        REFERENCES dynamic_layout_box (id)
);
