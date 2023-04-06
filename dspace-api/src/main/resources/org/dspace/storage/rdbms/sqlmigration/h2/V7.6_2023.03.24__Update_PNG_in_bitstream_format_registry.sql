--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Update short description for PNG mimetype in the bitstream format registry
-- See: https://github.com/DSpace/DSpace/pull/8722
-----------------------------------------------------------------------------------

UPDATE bitstreamformatregistry
SET short_description='PNG'
WHERE short_description='image/png'
  AND mimetype='image/png';
