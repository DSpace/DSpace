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

------------------------------------------------------
-- DS-1782 DSpace Needs Local Object Identifiers
------------------------------------------------------

CREATE TABLE ObjectID
(
 dspace_id CHAR(36) NOT NULL UNIQUE,
 object_id INTEGER NOT NULL,
 object_type INTEGER NOT NULL,
 CONSTRAINT objectid_pkey PRIMARY KEY (object_id, object_type)
);
CREATE INDEX object_id_idx ON ObjectID(dspace_id);

COMMIT;