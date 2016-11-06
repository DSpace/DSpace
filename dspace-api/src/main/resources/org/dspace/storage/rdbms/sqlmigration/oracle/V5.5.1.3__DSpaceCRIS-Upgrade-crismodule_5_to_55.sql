--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE cris_do_no ADD COLUMN preferred NUMBER(1,0);
ALTER TABLE cris_ou_no ADD COLUMN preferred NUMBER(1,0);
ALTER TABLE cris_pj_no ADD COLUMN preferred NUMBER(1,0);
ALTER TABLE cris_rp_no ADD COLUMN preferred NUMBER(1,0);
