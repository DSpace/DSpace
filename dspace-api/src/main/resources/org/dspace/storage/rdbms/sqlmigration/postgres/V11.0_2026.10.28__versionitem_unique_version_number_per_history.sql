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
WITH histories_with_duplicates AS (
    SELECT versionhistory_id
    FROM versionitem
    GROUP BY versionhistory_id, version_number
    HAVING COUNT(*) > 1
),
ranked AS (
    SELECT vi.versionitem_id,
           ROW_NUMBER() OVER (
               PARTITION BY vi.versionhistory_id
               ORDER BY vi.version_date ASC NULLS LAST, vi.versionitem_id ASC
           ) AS new_version_number
    FROM versionitem vi
    WHERE vi.versionhistory_id IN (SELECT versionhistory_id FROM histories_with_duplicates)
)
UPDATE versionitem
SET version_number = ranked.new_version_number
FROM ranked
WHERE versionitem.versionitem_id = ranked.versionitem_id;

-- Enforce uniqueness of version_number within a version history.
ALTER TABLE versionitem
    ADD CONSTRAINT versionitem_versionhistory_version_number_unique
        UNIQUE (versionhistory_id, version_number);
