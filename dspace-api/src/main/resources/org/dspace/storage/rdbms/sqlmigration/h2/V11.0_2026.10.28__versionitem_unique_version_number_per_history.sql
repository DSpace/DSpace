--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Ensure version_number is unique within a version history.
--
-- Concurrent "create new version" races could previously insert two Version rows
-- with the same version_number for the same versionhistory_id (each linked to a
-- different Item). Clean up any such duplicates first, then enforce uniqueness.
-----------------------------------------------------------------------------------

-- For each version history that already has duplicate version_number values,
-- renumber ALL of its versionitem rows sequentially (1..N) by version_date,
-- then versionitem_id as a stable tie-breaker.
-- (H2-friendly: stage new numbers in a temp table, then update.)
CREATE TABLE versionitem_renumber_tmp AS
SELECT versionitem_id,
       ROW_NUMBER() OVER (
           PARTITION BY versionhistory_id
           ORDER BY version_date ASC NULLS LAST, versionitem_id ASC
       ) AS new_version_number
FROM versionitem
WHERE versionhistory_id IN (
    SELECT versionhistory_id
    FROM versionitem
    GROUP BY versionhistory_id, version_number
    HAVING COUNT(*) > 1
);

UPDATE versionitem
SET version_number = (
    SELECT new_version_number
    FROM versionitem_renumber_tmp
    WHERE versionitem_renumber_tmp.versionitem_id = versionitem.versionitem_id
)
WHERE versionitem_id IN (SELECT versionitem_id FROM versionitem_renumber_tmp);

DROP TABLE versionitem_renumber_tmp;

-- Enforce uniqueness of version_number within a version history.
ALTER TABLE versionitem
    ADD CONSTRAINT versionitem_versionhistory_version_number_unique
        UNIQUE (versionhistory_id, version_number);
