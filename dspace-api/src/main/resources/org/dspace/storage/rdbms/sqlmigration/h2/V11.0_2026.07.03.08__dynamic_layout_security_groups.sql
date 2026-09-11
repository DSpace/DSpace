--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- CRIS Layout Security Groups - squashed idempotent migration
-- Consolidates: V7.2_2022.05.06, V7.2_2022.05.08,
--   V7.6_2023.12.12, V7.6_2023.12.13
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS dynamic_layout_box2securitygroup
(
    box_id             INTEGER NOT NULL,
    group_id           UUID NOT NULL,
    alternative_box_id INTEGER,
    CONSTRAINT dynamic_layout_box2securitygroup_pkey PRIMARY KEY (box_id, group_id),
    CONSTRAINT dynamic_layout_box2securitygroup_box_id
        FOREIGN KEY (box_id) REFERENCES dynamic_layout_box (id),
    CONSTRAINT dynamic_layout_box2securitygroup_group_id
        FOREIGN KEY (group_id) REFERENCES epersongroup (uuid),
    CONSTRAINT dynamic_layout_box2securitygroup_box_id2
        FOREIGN KEY (alternative_box_id) REFERENCES dynamic_layout_box (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS dynamic_layout_tab2securitygroup
(
    tab_id             INTEGER NOT NULL,
    group_id           UUID NOT NULL,
    alternative_tab_id INTEGER,
    CONSTRAINT dynamic_layout_tab2securitygroup_pkey PRIMARY KEY (tab_id, group_id),
    CONSTRAINT dynamic_layout_tab2securitygroup_tab_id
        FOREIGN KEY (tab_id) REFERENCES dynamic_layout_tab (id),
    CONSTRAINT dynamic_layout_tab2securitygroup_group_id
        FOREIGN KEY (group_id) REFERENCES epersongroup (uuid),
    CONSTRAINT dynamic_layout_tab2securitygroup_tab_id2
        FOREIGN KEY (alternative_tab_id) REFERENCES dynamic_layout_tab (id) ON DELETE SET NULL
);
