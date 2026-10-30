--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- Dynamic Layout Metric 2 Box - squashed idempotent migration
-- Consolidates: V7.0_2020.12.08 (metric2box), V7.0_2021.01.01 (metric2box id)
--
-- Matches the production DSpace-CRIS schema: a surrogate id column is the
-- primary key (the original 2020 composite key was replaced by (id) in 2021).
-------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS dynamic_layout_metric2box_id_seq;

CREATE TABLE IF NOT EXISTS dynamic_layout_metric2box
(
    id                   INTEGER NOT NULL,
    metric_type          VARCHAR(255) NOT NULL,
    dynamic_layout_box_id INTEGER NOT NULL,
    position             INTEGER NOT NULL,
    CONSTRAINT dynamic_layout_metric2box_pkey PRIMARY KEY (id),
    CONSTRAINT dynamic_layout_box2metric_box_id_fkey FOREIGN KEY (dynamic_layout_box_id)
        REFERENCES dynamic_layout_box (id)
);
