--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- Relationship no longer needs relationship type
ALTER TABLE relationship ALTER COLUMN type_id DROP NOT NULL;

-- Relationship can alternatively use configured semantics
ALTER TABLE relationship ADD COLUMN relationship_config_key VARCHAR(255);
CREATE INDEX relationship_config_key_idx ON relationship (relationship_config_key);

-- Metadata optionally references Relationship; no uniqueness constraint limits projection count.
ALTER TABLE metadatavalue ADD COLUMN relationship_id INTEGER;
ALTER TABLE metadatavalue
    ADD CONSTRAINT metadatavalue_relationship_id_fk
    FOREIGN KEY (relationship_id) REFERENCES relationship (id) ON DELETE SET NULL;
CREATE INDEX metadatavalue_relationship_id_idx ON metadatavalue (relationship_id);

-- SET NULL is only referential safety. Services implement detach/delete policy
-- and clear stale UUID authorities in managed objects before deleting a link.
