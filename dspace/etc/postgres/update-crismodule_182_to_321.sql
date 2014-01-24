alter table cris_do_wpointer add column urlPath varchar(255);
alter table cris_ou_wpointer add column urlPath varchar(255);
alter table cris_pj_wpointer add column urlPath varchar(255);
alter table cris_rp_wpointer add column urlPath varchar(255);

alter table cris_do add column sourceRef varchar(255);
alter table cris_orgunit add column sourceRef varchar(255);
alter table cris_project add column sourceRef varchar(255);
alter table cris_rpage add column sourceRef varchar(255)