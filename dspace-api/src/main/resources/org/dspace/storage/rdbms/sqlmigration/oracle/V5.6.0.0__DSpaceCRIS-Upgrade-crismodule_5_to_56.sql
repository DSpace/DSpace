--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE
'create table cris_do_box2policygroup (box_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_do_box2policysingle (box_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_do_tab2policygroup (tab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_do_tab2policysingle (tab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_ou_box2policygroup (box_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_ou_box2policysingle (box_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_ou_tab2policygroup (tab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_ou_tab2policysingle (tab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_pj_box2policygroup (box_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_pj_box2policysingle (box_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_pj_tab2policygroup (tab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_pj_tab2policysingle (tab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_rp_box2policygroup (box_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_rp_box2policysingle (box_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_rp_tab2policygroup (tab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_rp_tab2policysingle (tab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_do_etab2policygroup (tab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_do_etab2policysingle (tab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_ou_etab2policygroup (tab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_ou_etab2policysingle (tab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_pj_etab2policygroup (tab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_pj_etab2policysingle (tab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_rp_etab2policygroup (tab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_rp_etab2policysingle (tab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_weperson (id number(10,0) not null, filter clob, primary key (id));
create table cris_wgroup (id number(10,0) not null, filter clob, primary key (id));
alter table jdyna_values add customPointer number(10,0);
alter table cris_do_box2policygroup add constraint FK_fqxyx09rdfu2fdlml08828xk8 foreign key (box_id) references cris_do_box;
alter table cris_do_box2policysingle add constraint FK_mjladl11m2680hn8o4btb4lly foreign key (box_id) references cris_do_box;
alter table cris_do_tab2policygroup add constraint FK_j7d7vi6lj4h5wictl9sh6lmot foreign key (tab_id) references cris_do_tab;
alter table cris_do_tab2policysingle add constraint FK_i6f2ks46ta2j5vs00erqo2ip5 foreign key (tab_id) references cris_do_tab;
alter table cris_ou_box2policygroup add constraint FK_60gjy1oe2yrpm5180hfxaop3l foreign key (box_id) references cris_ou_box;
alter table cris_ou_box2policysingle add constraint FK_dllsgbtw3raif8edkidoeoi56 foreign key (box_id) references cris_ou_box;
alter table cris_ou_tab2policygroup add constraint FK_484l9q8ojjiuk522qwtj5c78w foreign key (tab_id) references cris_ou_tab;
alter table cris_ou_tab2policysingle add constraint FK_p3j0r630vykj5t6od5ysd4ikk foreign key (tab_id) references cris_ou_tab;
alter table cris_pj_box2policygroup add constraint FK_rbdf00op5aym7rchnqxtsc7bv foreign key (box_id) references cris_pj_box;
alter table cris_pj_box2policysingle add constraint FK_ol4361xb7guetm6mlrltokeur foreign key (box_id) references cris_pj_box;
alter table cris_pj_tab2policygroup add constraint FK_amf69chwwq9ktp2fbj0tagt3a foreign key (tab_id) references cris_pj_tab;
alter table cris_pj_tab2policysingle add constraint FK_456mu7altf23l6u163l255erc foreign key (tab_id) references cris_pj_tab;
alter table cris_rp_box2policygroup add constraint FK_pwxgxcddbve4x9h92iqtxxpff foreign key (box_id) references cris_rp_box;
alter table cris_rp_box2policysingle add constraint FK_ritnnjkrjlp044yjo0tfpt19f foreign key (box_id) references cris_rp_box;
alter table cris_rp_tab2policygroup add constraint FK_ajnk574oowmgpud4o9kpctlp foreign key (tab_id) references cris_rp_tab;
alter table cris_rp_tab2policysingle add constraint FK_7dme5a998b5cffs2ynyk94l5d foreign key (tab_id) references cris_rp_tab;
alter table cris_do_etab2policygroup add constraint FK_notgna4379iv6h6gh90vqvgg2 foreign key (etab_id) references cris_do_etab;
alter table cris_do_etab2policysingle add constraint FK_9c541j5og0fn26aiig2ud2k2d foreign key (etab_id) references cris_do_etab;
alter table cris_ou_etab2policygroup add constraint FK_ctyvoar08kmyrksr21fs3wjch foreign key (etab_id) references cris_ou_etab;
alter table cris_ou_etab2policysingle add constraint FK_6cw0xrjng41aedd4mk76jcm39 foreign key (etab_id) references cris_ou_etab;
alter table cris_pj_etab2policygroup add constraint FK_cpnidos6chf15smpf365k9u2d foreign key (etab_id) references cris_pj_etab;
alter table cris_pj_etab2policysingle add constraint FK_c370hgq2gwt1fk1cpn92y5wof foreign key (etab_id) references cris_pj_etab;
alter table cris_rp_etab2policygroup add constraint FK_i8ye9656ab432x6ylqvi3eiek foreign key (etab_id) references cris_rp_etab;
alter table cris_rp_etab2policysingle add constraint FK_r9o249od95444ipvgrnij3uvl foreign key (etab_id) references cris_rp_etab;'
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;