--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--


--------------------------------------------------------
-- repair script for corrupted bundle2bitstream tables.
--------------------------------------------------------

UPDATE bitstream
SET deleted = false
WHERE uuid IN (
    SELECT b.uuid
    FROM bitstream b
             INNER JOIN bundle2bitstream b2b ON b.uuid = b2b.bitstream_id
             INNER JOIN item2bundle i2b      ON b2b.bundle_id = i2b.bundle_id
    WHERE b.deleted = true
);

DELETE FROM bundle2bitstream a
    USING (
    SELECT MAX(ctid) AS ctid, bundle_id, bitstream_id
    FROM bundle2bitstream
    GROUP BY bundle_id, bitstream_id
    HAVING COUNT(*) > 1
) b
WHERE a.bundle_id    = b.bundle_id
  AND a.bitstream_id = b.bitstream_id
  AND a.ctid        <> b.ctid;

ALTER TABLE bundle2bitstream ADD COLUMN bitstream_order_fix INT;

WITH numbered AS (
    SELECT bundle_id,
           bitstream_id,
           ROW_NUMBER() OVER (PARTITION BY bundle_id ORDER BY bitstream_order) - 1 AS rn
    FROM bundle2bitstream
)
UPDATE bundle2bitstream
SET bitstream_order_fix = numbered.rn
    FROM numbered
WHERE bundle2bitstream.bundle_id    = numbered.bundle_id
  AND bundle2bitstream.bitstream_id = numbered.bitstream_id;

ALTER TABLE bundle2bitstream DROP COLUMN bitstream_order;
ALTER TABLE bundle2bitstream RENAME COLUMN bitstream_order_fix TO bitstream_order;
