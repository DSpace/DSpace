--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- ALTER table registrationdata
-----------------------------------------------------------------------------------

EXECUTE IMMEDIATE 'ALTER TABLE registrationdata DROP CONSTRAINT ' ||
 QUOTE_IDENT((SELECT CONSTRAINT_NAME
        FROM information_schema.key_column_usage
        WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'REGISTRATIONDATA' AND COLUMN_NAME = 'EMAIL'));

ALTER TABLE registrationdata
ADD COLUMN registration_type VARCHAR2(255);

ALTER TABLE registrationdata
ADD COLUMN net_id VARCHAR2(64);

CREATE SEQUENCE  IF NOT EXISTS registrationdata_metadatavalue_seq START WITH 1 INCREMENT BY 1;

-----------------------------------------------------------------------------------
-- Creates table registrationdata_metadata
-----------------------------------------------------------------------------------

CREATE TABLE registrationdata_metadata (
  registrationdata_metadata_id INTEGER NOT NULL,
  registrationdata_id INTEGER,
  metadata_field_id INTEGER,
  text_value VARCHAR2(2000),
  CONSTRAINT pk_registrationdata_metadata PRIMARY KEY (registrationdata_metadata_id)
);

ALTER TABLE registrationdata_metadata
ADD CONSTRAINT FK_REGISTRATIONDATA_METADATA_ON_METADATA_FIELD
    FOREIGN KEY (metadata_field_id)
    REFERENCES metadatafieldregistry (metadata_field_id) ON DELETE CASCADE;

ALTER TABLE registrationdata_metadata
ADD CONSTRAINT FK_REGISTRATIONDATA_METADATA_ON_REGISTRATIONDATA
    FOREIGN KEY (registrationdata_id)
    REFERENCES registrationdata (registrationdata_id) ON DELETE CASCADE;
