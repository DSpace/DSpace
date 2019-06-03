--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- ===============================================================
-- WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
--
-- DO NOT MANUALLY RUN THIS DATABASE MIGRATION. IT WILL BE EXECUTED
-- AUTOMATICALLY (IF NEEDED) BY "FLYWAY" WHEN YOU STARTUP DSPACE.
-- http://flywaydb.org/
-- ===============================================================

----------------------------------------------------------------------------------------------------------------
-- This adds TYPE_INHERITED to all old archived items permission due to the change on resource policy management
----------------------------------------------------------------------------------------------------------------
UPDATE resourcepolicy set rptype = 'TYPE_INHERITED' 
	where resource_type_id = 2 and rptype is null 
		and dspace_object in (
			select uuid from item where in_archive = 1
			);