--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- add community UUIDs to dspace_object_ids (in community subscriptions)
-----------------------------------------------------------------------------------

UPDATE subscription SET dspace_object_id = (SELECT uuid FROM community WHERE community_id = subscription.community_id) WHERE community_id IS NOT NULL;
