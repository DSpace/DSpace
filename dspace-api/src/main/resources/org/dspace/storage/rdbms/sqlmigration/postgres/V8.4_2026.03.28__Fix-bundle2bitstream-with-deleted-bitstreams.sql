--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- Remove orphaned bundle2bitstream rows that reference deleted bitstreams.
-- These orphaned rows prevent 'dspace cleanup' from expunging deleted
-- bitstreams due to FK constraint violations.
DELETE
FROM bundle2bitstream
WHERE bitstream_id IN
      (SELECT bs.uuid
       FROM bitstream AS bs
       WHERE bs.deleted IS TRUE)
