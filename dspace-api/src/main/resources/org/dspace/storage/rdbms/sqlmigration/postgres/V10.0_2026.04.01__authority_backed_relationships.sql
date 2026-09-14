--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Authority-backed relationships: persistence substrate (additive, inert).
--   1. Make relationship.type_id nullable so a type-less relationship row may exist.
--   2. Add metadatavalue.relationship_id so a metadata value can name the one
--      relationship row it owns (FK ON DELETE SET NULL as a belt-and-suspenders
--      safeguard; real teardown happens in the service layer).
-- Nothing populates these yet; existing rows keep type_id set and relationship_id NULL.
-- The existing UNIQUE (left_id, type_id, right_id) constraint is intentionally left
-- unchanged: under Postgres NULL semantics a NULL type_id yields distinct rows, so
-- the editor and the author of the same person are two separate rows.
-----------------------------------------------------------------------------------

-- 1. relationship.type_id becomes nullable
ALTER TABLE relationship ALTER COLUMN type_id DROP NOT NULL;

-- 2. metadatavalue.relationship_id: owning link back to the single relationship row
ALTER TABLE metadatavalue ADD COLUMN IF NOT EXISTS relationship_id INTEGER;

ALTER TABLE metadatavalue
    ADD CONSTRAINT metadatavalue_relationship_id_fk
    FOREIGN KEY (relationship_id) REFERENCES relationship (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS metadatavalue_relationship_id_idx
    ON metadatavalue (relationship_id);
