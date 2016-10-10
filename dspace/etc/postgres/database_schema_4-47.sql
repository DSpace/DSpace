--
-- database_schema_4-47.sql
--
-- Version: $Revision$
--
-- Date:    $Date: 2016-10-07
--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--
-- SQL commands to upgrade the database schema of a live DSpace 4 <
-- to the DSpace 4.7 database schema
--
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
--

------------------------------------------------------
-- DS-3097 introduced new action id for WITHDRAWN_READ 
------------------------------------------------------

UPDATE resourcepolicy SET action_id = 12 where action_id = 0 and resource_type_id = 0 and resource_id in (
	SELECT bundle2bitstream.bitstream_id FROM bundle2bitstream
		LEFT JOIN item2bundle ON bundle2bitstream.bundle_id = item2bundle.bundle_id
		LEFT JOIN item ON item2bundle.item_id = item.item_id
		WHERE item.withdrawn = true
);

UPDATE resourcepolicy SET action_id = 12 where action_id = 0 and resource_type_id = 1 and resource_id in (
	SELECT item2bundle.bundle_id FROM item2bundle
		LEFT JOIN item ON item2bundle.item_id = item.item_id
		WHERE item.withdrawn = true
);