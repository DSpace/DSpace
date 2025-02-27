--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- Add new access_token column to hold a secure access token for the requestor to use for weblink-based access
ALTER TABLE requestitem ADD COLUMN IF NOT EXISTS access_token VARCHAR(48);
-- Add new access_period column to hold a time delta in seconds (from decision_date timestamp) to calculate validity
-- and expiry, with NULL interpreted as 'forever' (if accept_request is true). int4 allows for 68 year period max.
ALTER TABLE requestitem ADD COLUMN IF NOT EXISTS access_period INT4;