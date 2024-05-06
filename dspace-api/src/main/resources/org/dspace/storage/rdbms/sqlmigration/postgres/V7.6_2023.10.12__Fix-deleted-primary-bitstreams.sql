--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN;

-- Unset any primary bitstream that is marked as deleted
UPDATE bundle
SET primary_bitstream_id = NULL
WHERE primary_bitstream_id IN
    ( SELECT bs.uuid
           FROM bitstream AS bs
           INNER JOIN bundle as bl ON bs.uuid = bl.primary_bitstream_id
           WHERE bs.deleted IS TRUE );

-- Unset any primary bitstream that don't belong to bundle's bitstream list
UPDATE bundle
SET primary_bitstream_id = NULL
WHERE primary_bitstream_id IN
    ( SELECT bl.primary_bitstream_id
           FROM bundle as bl
           WHERE bl.primary_bitstream_id IS NOT NULL
               AND bl.primary_bitstream_id NOT IN
                   ( SELECT bitstream_id
                          FROM bundle2bitstream AS b2b
                          WHERE b2b.bundle_id = bl.uuid
                   )
    );

COMMIT;
