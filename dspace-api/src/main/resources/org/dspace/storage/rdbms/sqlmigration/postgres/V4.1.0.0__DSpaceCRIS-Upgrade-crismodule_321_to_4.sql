alter table cris_do add column sourceRef varchar(255);
alter table cris_do_no add column sourceID varchar(255);
alter table cris_do_no add column sourceRef varchar(255);
alter table cris_orgunit add column sourceRef varchar(255);
alter table cris_ou_no add column sourceID varchar(255);
alter table cris_ou_no add column sourceRef varchar(255);
alter table cris_pj_no add column sourceID varchar(255);
alter table cris_pj_no add column sourceRef varchar(255);
alter table cris_project add column sourceRef varchar(255);
alter table cris_rp_no add column sourceID varchar(255);
alter table cris_rp_no add column sourceRef varchar(255);
alter table cris_rpage add column sourceRef varchar(255);

ALTER TABLE cris_do ADD CONSTRAINT cris_do_sourceid_sourceref_key UNIQUE (sourceid, sourceref);
ALTER TABLE cris_orgunit ADD CONSTRAINT cris_orgunit_sourceid_sourceref_key UNIQUE (sourceid, sourceref);
ALTER TABLE cris_project ADD CONSTRAINT cris_project_sourceid_sourceref_key UNIQUE (sourceid, sourceref);
ALTER TABLE cris_rpage ADD CONSTRAINT cris_rpage_sourceid_sourceref_key UNIQUE (sourceid, sourceref);