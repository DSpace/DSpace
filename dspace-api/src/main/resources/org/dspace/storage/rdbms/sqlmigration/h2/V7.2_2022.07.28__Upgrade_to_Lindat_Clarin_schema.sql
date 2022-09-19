--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE handle ADD url varchar(2048);
ALTER TABLE handle ADD dead BOOL;
ALTER TABLE handle ADD dead_since TIMESTAMP;
