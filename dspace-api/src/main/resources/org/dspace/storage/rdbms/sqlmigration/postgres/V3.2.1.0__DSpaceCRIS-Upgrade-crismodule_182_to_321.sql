--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

alter table cris_do_wpointer add column urlPath varchar(255);
alter table cris_ou_wpointer add column urlPath varchar(255);
alter table cris_pj_wpointer add column urlPath varchar(255);
alter table cris_rp_wpointer add column urlPath varchar(255);