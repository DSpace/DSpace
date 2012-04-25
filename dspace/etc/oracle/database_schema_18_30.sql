CREATE SEQUENCE rmetadatafieldregistry_seq;
CREATE SEQUENCE rmetadatavalue_seq;


CREATE TABLE RMetadataFieldRegistry
(
  metadata_field_id   INTEGER PRIMARY KEY,
  resource_type_id  INTEGER,
  element    VARCHAR(64),
  qualifier  VARCHAR(64),
  scope_note VARCHAR2(2000)
);

CREATE TABLE RMetadataValue
(
  metadata_value_id  INTEGER PRIMARY KEY,
  resource_id        INTEGER,
  resource_type_id   INTEGER,
  metadata_field_id  INTEGER REFERENCES RMetadataFieldRegistry(metadata_field_id),
  text_value         CLOB,
  text_lang          VARCHAR(64),
  place              INTEGER
);


CREATE INDEX rmetadatavalue_resource_idx ON RMetadataValue(resource_id,resource_type_id);
CREATE INDEX rmetadatavalue_resource_idx2 ON RMetadataValue(resource_id,resource_type_id,metadata_field_id);
CREATE INDEX rmetadatavalue_field_fk_idx ON RMetadataValue(metadata_field_id);
CREATE INDEX rmetadatafield_schema_idx ON RMetadataFieldRegistry(resource_type_id);

