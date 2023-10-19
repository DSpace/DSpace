BEGIN;

-- Remove all primary bitstreams that are marked as deleted
UPDATE bundle
SET primary_bitstream_id = NULL
WHERE primary_bitstream_id IN
    ( SELECT bs.uuid
           FROM bitstream AS bs
           INNER JOIN bundle as bl ON bs.uuid = bl.primary_bitstream_id
           WHERE bs.deleted IS TRUE );

-- Remove all primary bitstreams that don't make part on bundle's bitstreams
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