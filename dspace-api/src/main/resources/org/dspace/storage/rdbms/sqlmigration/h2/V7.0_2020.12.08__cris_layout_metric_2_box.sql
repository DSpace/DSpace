--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Metric 2 Box Table
-----------------------------------------------------------------------------------

CREATE TABLE cris_layout_metric2box
(
    metric_type CHARACTER VARYING(255) NOT NULL,
    cris_layout_box_id INTEGER NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT cris_layout_metric2box_pkey PRIMARY KEY (cris_layout_box_id, metric_type),
    CONSTRAINT cris_layout_box2metric_box_id_fkey FOREIGN KEY (cris_layout_box_id)
        REFERENCES cris_layout_box (id)
);

ALTER TABLE cris_layout_box ADD COLUMN max_columns INTEGER;

