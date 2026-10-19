--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- Add new access_token column to hold a secure access token for the requestor to use for weblink-based access
ALTER TABLE requestitem ADD COLUMN IF NOT EXISTS access_token VARCHAR(48);
-- Add new access_expiry DATESTAMP column to hold the expiry date of the access token
-- (note this is separate from the existing 'expires' column which was intended as the expiry date of the request itself)
ALTER TABLE requestitem ADD COLUMN IF NOT EXISTS access_expiry TIMESTAMP;
