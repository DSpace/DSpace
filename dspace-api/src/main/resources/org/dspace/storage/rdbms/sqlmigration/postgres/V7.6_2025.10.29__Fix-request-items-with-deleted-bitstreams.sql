--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

DELETE
FROM requestitem
WHERE bitstream_id IN
      (SELECT bs.uuid
       FROM bitstream AS bs
       WHERE bs.deleted IS TRUE)
