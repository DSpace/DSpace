CREATE SEQUENCE resourcemetadatafieldregistry_seq;
CREATE SEQUENCE resourcemetadatavalue_seq;


CREATE TABLE ResourceMetadataFieldRegistry
(
  metadata_field_id   INTEGER PRIMARY KEY DEFAULT NEXTVAL('resourcemetadatafieldregistry_seq'),
  resource_type_id    INTEGER,
  element             VARCHAR(64),
  qualifier           VARCHAR(64),
  scope_note          TEXT
);

CREATE TABLE ResourceMetadataValue
(
  metadata_value_id  INTEGER PRIMARY KEY DEFAULT NEXTVAL('resourcemetadatavalue_seq'),
  resource_id        INTEGER,
  resource_type_id   INTEGER,
  metadata_field_id  INTEGER REFERENCES ResourceMetadataFieldRegistry(metadata_field_id),
  text_value         TEXT,
  text_lang          VARCHAR(24),
  place              INTEGER
);


CREATE INDEX resourcemetadatavalue_resource_idx ON ResourceMetadataValue(resource_id,resource_type_id);
CREATE INDEX resourcemetadatavalue_resource_idx2 ON ResourceMetadataValue(resource_id,resource_type_id,metadata_field_id);
CREATE INDEX resourcemetadatavalue_field_fk_idx ON ResourceMetadataValue(metadata_field_id);
CREATE INDEX resourcemetadatafield_schema_idx ON ResourceMetadataFieldRegistry(resource_type_id);
