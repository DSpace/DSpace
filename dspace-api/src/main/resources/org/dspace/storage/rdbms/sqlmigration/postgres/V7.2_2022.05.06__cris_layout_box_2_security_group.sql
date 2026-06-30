--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
CREATE TABLE if not exists public.cris_layout_box2securitygroup
(
    box_id integer NOT NULL,
    group_id uuid NOT NULL,
    CONSTRAINT cris_layout_box2securitygroup_pkey PRIMARY KEY (box_id, group_id),
    CONSTRAINT cris_layout_box2securitygroup_box_id FOREIGN KEY (box_id) REFERENCES public.cris_layout_box (id),
    CONSTRAINT cris_layout_box2securitygroup_group_id FOREIGN KEY (group_id) REFERENCES public.epersongroup (uuid)
);