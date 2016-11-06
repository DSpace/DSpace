--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

alter table cris_do_no add column preferred boolean;
alter table cris_ou_no add column preferred boolean;
alter table cris_pj_no add column preferred boolean;
alter table cris_rp_no add column preferred boolean;
