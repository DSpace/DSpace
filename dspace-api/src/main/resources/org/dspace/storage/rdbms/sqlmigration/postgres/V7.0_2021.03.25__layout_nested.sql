--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------------------------------
-- Sequences for RegistrationData within Group feature
-------------------------------------------------------------------------------

CREATE SEQUENCE cris_layout_field_nested_id_seq;
CREATE TABLE  cris_layout_field2nested
(
  nested_field_id    INTEGER NOT NULL,
  rendering           CHARACTER VARYING(255),
  priority            INTEGER NOT NULL,
  label               CHARACTER VARYING(255),
  style               CHARACTER VARYING(255),
  style_label         CHARACTER VARYING(255),
  style_value         CHARACTER VARYING(255),
  metadata_field_id   INTEGER  NOT NULL,
  field_id            INTEGER  NOT NULL,
  CONSTRAINT cris_layout_field2nested_pkey PRIMARY KEY (nested_field_id),
  CONSTRAINT cris_layout_field2nested_metadatafieldregistry_fkey  FOREIGN KEY  (metadata_field_id)
  REFERENCES metadatafieldregistry (metadata_field_id),
  CONSTRAINT cris_layout_field2nested_cris_layout_field_fkey FOREIGN KEY (field_id) REFERENCES cris_layout_field (field_id)
);