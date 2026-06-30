--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create tables for Tabs/Boxes Configuration
-----------------------------------------------------------------------------------

CREATE SEQUENCE cris_layout_box_id_seq;

CREATE TABLE cris_layout_box
(
    id INTEGER NOT NULL,
    entity_id INTEGER NOT NULL,
    type VARCHAR(255),
    collapsed boolean NOT NULL,
    priority INTEGER NOT NULL,
    shortname VARCHAR(255),
    header VARCHAR(255),
    minor BOOLEAN NOT NULL,
    security INTEGER,
    style VARCHAR(255),
    clear BOOLEAN,
    CONSTRAINT cris_layout_box_pkey PRIMARY KEY (id),
    CONSTRAINT cris_layout_box_shortname_key UNIQUE (shortname),
    CONSTRAINT cris_layout_box_entity_id_fkey FOREIGN KEY (entity_id)
        REFERENCES entity_type (id)
);

CREATE TABLE cris_layout_box2securityfield
(
    box_id INTEGER NOT NULL,
    authorized_field_id INTEGER NOT NULL,   
    CONSTRAINT cris_layout_box2securityfield_box_id_fkey FOREIGN KEY (box_id)
        REFERENCES cris_layout_box (id),
    CONSTRAINT cris_layout_box2securityfield_field_id_fkey FOREIGN KEY (authorized_field_id)
        REFERENCES metadatafieldregistry (metadata_field_id)
);

CREATE SEQUENCE cris_layout_field_field_id_seq;

CREATE TABLE cris_layout_field
(
    field_id INTEGER NOT NULL,
    metadata_field_id INTEGER NOT NULL,
    bundle VARCHAR(255),
    rendering VARCHAR(255),
    row INTEGER NOT NULL, 
    priority INTEGER NOT NULL,
    type VARCHAR(255),
    label VARCHAR(255),
    style VARCHAR(255),
    CONSTRAINT cris_layout_field_pkey PRIMARY KEY (field_id),
    CONSTRAINT cris_layout_box2metadata_metadata_field_id_fkey FOREIGN KEY (metadata_field_id)
        REFERENCES metadatafieldregistry (metadata_field_id)
);

CREATE TABLE cris_layout_box2field
(
    cris_layout_box_id INTEGER NOT NULL,
    cris_layout_field_id INTEGER NOT NULL,
    CONSTRAINT cris_layout_box2field_field_id_fkey FOREIGN KEY (cris_layout_field_id)
        REFERENCES cris_layout_field (field_id),
    CONSTRAINT cris_layout_box2field_box_id_fkey FOREIGN KEY (cris_layout_box_id)
        REFERENCES cris_layout_box (id)
);

CREATE SEQUENCE cris_layout_fieldbitstream2metadata_fieldbitstream_id_seq;

CREATE TABLE cris_layout_fieldbitstream2metadata 
(
    fieldbitstream_id INTEGER NOT NULL,
    layout_field_id INTEGER NOT NULL,
    metadata_field_id INTEGER NOT NULL,
    bundle VARCHAR(255),
    metadata_value VARCHAR(255),
    CONSTRAINT cris_layout_fieldbitstream2metadata_pkey PRIMARY KEY (fieldbitstream_id),
    CONSTRAINT cris_layout_fieldbitstream2metadata_layout_field_id_fkey FOREIGN KEY (layout_field_id)
        REFERENCES cris_layout_field (field_id),
    CONSTRAINT cris_layout_fieldbitstream2metadata_field_id_fkey FOREIGN KEY (metadata_field_id)
        REFERENCES metadatafieldregistry (metadata_field_id)
);

CREATE SEQUENCE cris_layout_tab_id_seq;

CREATE TABLE cris_layout_tab
(
    id INTEGER NOT NULL,
    entity_id INTEGER NOT NULL,
    priority INTEGER NOT NULL,
    shortname VARCHAR(255),
    header VARCHAR(255),
    security INTEGER,
    CONSTRAINT cris_layout_tab_pkey PRIMARY KEY (id),
    CONSTRAINT cris_layout_tab_shortname_key UNIQUE (shortname),
    CONSTRAINT cris_layout_tab_entity_id_fkey FOREIGN KEY (entity_id)
        REFERENCES entity_type (id)
);

CREATE TABLE cris_layout_tab2box
(
    cris_layout_tab_id INTEGER NOT NULL,
    cris_layout_box_id INTEGER NOT NULL,
    CONSTRAINT cris_layout_tab2box_tab_id_fkey FOREIGN KEY (cris_layout_tab_id)
        REFERENCES cris_layout_tab (id),
    CONSTRAINT cris_layout_tab2box_box_id_fkey FOREIGN KEY (cris_layout_box_id)
        REFERENCES cris_layout_box (id)
);

CREATE TABLE cris_layout_tab2securityfield
(
    tab_id INTEGER NOT NULL,
    authorized_field_id INTEGER NOT NULL,   
    CONSTRAINT cris_layout_tab2securityfield_tab_id_fkey FOREIGN KEY (tab_id)
        REFERENCES cris_layout_tab (id),
    CONSTRAINT cris_layout_tab2securityfield_field_id_fkey FOREIGN KEY (authorized_field_id)
        REFERENCES metadatafieldregistry (metadata_field_id)
);