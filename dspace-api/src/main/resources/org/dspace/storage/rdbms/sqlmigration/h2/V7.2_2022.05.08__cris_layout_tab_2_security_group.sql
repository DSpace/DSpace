--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
CREATE TABLE if not exists cris_layout_tab2securitygroup
(
    tab_id integer NOT NULL,
    group_id uuid NOT NULL,
    CONSTRAINT cris_layout_tab2securitygroup_pkey PRIMARY KEY (tab_id, group_id),
    CONSTRAINT cris_layout_tab2securitygroup_tab_id FOREIGN KEY (tab_id) REFERENCES cris_layout_tab (id),
    CONSTRAINT cris_layout_tab2securitygroup_group_id FOREIGN KEY (group_id) REFERENCES epersongroup (uuid)
);