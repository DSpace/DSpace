alter table cris_organizationunit add column crisID varchar(255) unique;
alter table cris_project add column crisID varchar(255) unique;
alter table cris_researcherpage add column crisID varchar(255) unique;
alter table cris_ou_nestedobject_prop add column lock int4;
alter table cris_ou_nestedobject_prop add column scope varchar(255);
alter table cris_ou_prop add column lock int4;
alter table cris_ou_prop add column scope varchar(255);
alter table cris_project_nestedobject_prop add column lock int4;
alter table cris_project_nestedobject_prop add column scope varchar(255);
alter table cris_project_prop add column lock int4;
alter table cris_project_prop add column scope varchar(255);
alter table cris_rp_nestedobject_prop add column lock int4;
alter table cris_rp_nestedobject_prop add column scope varchar(255);
alter table cris_rp_prop add column lock int4;
alter table cris_rp_prop add column scope varchar(255);
alter table jdyna_nestedobject_prop add column lock int4;
alter table jdyna_nestedobject_prop add column scope varchar(255);
alter table jdyna_containables add column externalJSP varchar(255);

create table cris_relationpreference (id int4 not null, itemID int4, priority int4 not null, relationType varchar(255), sourceUUID varchar(255), status varchar(255), targetUUID varchar(255), primary key (id));
create sequence CRIS_RELATIONPREFERENCE_SEQ;

DROP TABLE cris_researcherpage_rejectItems;

CREATE TABLE potentialmatches
(
   potentialmatches_id integer, 
   item_id integer, 
   rp character varying(20), 
    PRIMARY KEY (potentialmatches_id)
);
CREATE SEQUENCE potentialmatches_seq;
CREATE INDEX rp_idx
   ON potentialmatches (rp ASC NULLS LAST);
   
create table jdyna_box_message (id int4 not null, body varchar(255), elementAfter varchar(255), showInEdit bool not null, showInPublicView bool not null, useBodyAsKeyMessageBundle bool not null, parent_id int4, primary key (id));
create sequence JDYNA_MESSAGEBOX_SEQ;

alter table cris_ou_widgetfile add column useInStatistics bool not null default false;
alter table cris_project_widgetfile add column useInStatistics bool not null default false;
alter table cris_rp_widgetfile add column useInStatistics bool not null default false;

alter table cris_ou_nestedobject add column position int4;
alter table cris_project_nestedobject add column position int4;
alter table cris_rp_nestedobject add column position int4;
alter table jdyna_nestedobject add column position int4;

alter table StatSubscription add column type int4;
alter table StatSubscription add column uid varchar(255);
alter table StatSubscription RENAME COLUMN type to typeDef;
alter table StatSubscription RENAME COLUMN uid to handle_or_uuid;
ALTER TABLE StatSubscription RENAME TO cris_statsubscription;
ALTER TABLE statsubscription_seq RENAME TO CRIS_STATSUBSCRIPTION_SEQ;

create table cris_subscription (id int4 not null, epersonID int4 not null, typeDef int4, uuid varchar(255), primary key (id));
create sequence CRIS_SUBSCRIPTION_SEQ;

create table jdyna_scopedefinition (id int4 not null, label varchar(255), primary key (id));

ALTER TABLE cris_organizationunit RENAME TO cris_orgunit;
ALTER TABLE cris_ou_box2containable RENAME TO cris_ou_box2con;

ALTER TABLE cris_ou_edittab RENAME TO cris_ou_etab;
ALTER TABLE cris_ou_edittab2box RENAME TO cris_ou_etab2box;
ALTER TABLE cris_ou_etab2box RENAME COLUMN cris_ou_edittab_id to cris_ou_etab_id;

ALTER TABLE cris_ou_nestedobject RENAME TO cris_ou_no;
ALTER TABLE cris_ou_no DROP CONSTRAINT cris_ou_nestedobject_position_typo_id_parent_id_key;
ALTER TABLE cris_ou_no RENAME COLUMN position to positionDef;
alter table cris_ou_no ADD COLUMN scopeDef_id int4;
ALTER TABLE cris_ou_no ADD CONSTRAINT cris_ou_no_positiondef_typo_id_parent_id_key UNIQUE (positionDef, typo_id, parent_id);
alter table cris_ou_no add constraint FKC3CB15F7A7AEA5D2861768d4 foreign key (scopeDef_id) references jdyna_scopedefinition;

ALTER TABLE cris_ou_nestedobject_propertiesdefinition RENAME TO cris_ou_no_pdef;
ALTER TABLE cris_ou_nestedobject_prop RENAME TO cris_ou_no_prop;
ALTER TABLE cris_ou_no_prop DROP CONSTRAINT cris_ou_nestedobject_prop_position_typo_id_parent_id_key;
ALTER TABLE cris_ou_no_prop ADD endDate timestamp;
ALTER TABLE cris_ou_no_prop ADD startDate timestamp;
ALTER TABLE cris_ou_no_prop RENAME COLUMN lock to lockDef;
ALTER TABLE cris_ou_no_prop RENAME COLUMN position to positionDef;
alter table cris_ou_no_prop ADD COLUMN scopeDef_id int4;
alter table cris_ou_no_prop DROP COLUMN scope;
ALTER TABLE cris_ou_no_prop ADD CONSTRAINT cris_ou_no_prop_positiondef_typo_id_parent_id_key UNIQUE (positionDef, typo_id, parent_id);
alter table cris_ou_no_prop add constraint FKC8A841F5A7AEA5D2977c57ee foreign key (scopeDef_id) references jdyna_scopedefinition;

ALTER TABLE cris_ou_nestedobject_typo RENAME TO cris_ou_no_tp;
ALTER TABLE cris_ou_nestedobject_typo2mask RENAME TO cris_ou_no_tp2pdef;
ALTER TABLE cris_ou_no_tp2pdef RENAME COLUMN cris_ou_nestedobject_typo_id to cris_ou_no_tp_id;

ALTER TABLE cris_ou_propertiesdefinition RENAME TO cris_ou_pdef;

alter table cris_ou_prop add column endDate timestamp;
alter table cris_ou_prop add column startDate timestamp;
alter table cris_ou_prop RENAME COLUMN lock to lockDef;
alter table cris_ou_prop RENAME COLUMN position to positionDef;
alter table cris_ou_prop ADD COLUMN scopeDef_id int4;
alter table cris_ou_prop DROP COLUMN scope;
alter table cris_ou_prop add constraint FKC8A841F5A7AEA5D25de185b6 foreign key (scopeDef_id) references jdyna_scopedefinition;

ALTER TABLE cris_ou_widgetfile RENAME TO cris_ou_wfile;
ALTER TABLE cris_ou_wfile RENAME COLUMN size to widgetSize;

ALTER TABLE cris_project_box RENAME TO cris_pj_box;
ALTER TABLE cris_project_box2containable RENAME TO cris_pj_box2con;
ALTER TABLE cris_pj_box2con RENAME COLUMN cris_project_box_id to cris_pj_box_id;

ALTER TABLE cris_project_edittab RENAME TO cris_pj_etab;
ALTER TABLE cris_project_edittab2box RENAME TO cris_pj_etab2box;
ALTER TABLE cris_pj_etab2box RENAME COLUMN cris_project_edittab_id to cris_pj_etab_id;

ALTER TABLE cris_project_nestedobject RENAME TO cris_pj_no;
ALTER TABLE cris_pj_no DROP CONSTRAINT cris_project_nestedobject_position_typo_id_parent_id_key;
ALTER TABLE cris_pj_no RENAME COLUMN position to positionDef;
alter table cris_pj_no ADD COLUMN scopeDef_id int4;
ALTER TABLE cris_pj_no ADD CONSTRAINT cris_pj_no_positiondef_typo_id_parent_id_key UNIQUE (positionDef, typo_id, parent_id);
alter table cris_pj_no add constraint FKC3CB15F7A7AEA5D286208040 foreign key (scopeDef_id) references jdyna_scopedefinition;

ALTER TABLE cris_project_nestedobject_propertiesdefinition RENAME TO cris_pj_no_pdef;
ALTER TABLE cris_project_nestedobject_prop RENAME TO cris_pj_no_prop;
ALTER TABLE cris_pj_no_prop DROP CONSTRAINT cris_project_nestedobject_prop_position_typo_id_parent_id_key;
ALTER TABLE cris_pj_no_prop ADD endDate timestamp;
ALTER TABLE cris_pj_no_prop ADD startDate timestamp;
ALTER TABLE cris_pj_no_prop RENAME COLUMN lock to lockDef;
ALTER TABLE cris_pj_no_prop RENAME COLUMN position to positionDef;
alter table cris_pj_no_prop ADD COLUMN scopeDef_id int4;
alter table cris_pj_no_prop DROP COLUMN scope;
ALTER TABLE cris_pj_no_prop ADD CONSTRAINT cris_pj_no_prop_positiondef_typo_id_parent_id_key UNIQUE (positionDef, typo_id, parent_id);
alter table cris_pj_no_prop add constraint FKC8A841F5A7AEA5D22cd50402 foreign key (scopeDef_id) references jdyna_scopedefinition;

ALTER TABLE cris_project_nestedobject_typo RENAME TO cris_pj_no_tp;
ALTER TABLE cris_project_nestedobject_typo2mask RENAME TO cris_pj_no_tp2pdef;
ALTER TABLE cris_pj_no_tp2pdef RENAME COLUMN cris_project_nestedobject_typo_id to cris_pj_no_tp_id;

ALTER TABLE cris_project_propertiesdefinition RENAME TO cris_pj_pdef;
ALTER TABLE cris_project_prop RENAME TO cris_pj_prop;
alter table cris_pj_prop add column endDate timestamp;
alter table cris_pj_prop add column startDate timestamp;
alter table cris_pj_prop RENAME COLUMN lock to lockDef;
alter table cris_pj_prop RENAME COLUMN position to positionDef;
alter table cris_pj_prop ADD COLUMN scopeDef_id int4;
alter table cris_pj_prop DROP COLUMN scope;
alter table cris_pj_prop add constraint FKC8A841F5A7AEA5D280027222 foreign key (scopeDef_id) references jdyna_scopedefinition;

ALTER TABLE cris_project_tab RENAME TO cris_pj_tab;
ALTER TABLE cris_project_tab2box RENAME TO cris_pj_tab2box;
ALTER TABLE cris_pj_tab2box RENAME COLUMN cris_project_tab_id to cris_pj_tab_id;

ALTER TABLE cris_project_widgetfile RENAME TO cris_pj_wfile;
ALTER TABLE cris_pj_wfile RENAME COLUMN size to widgetSize;

ALTER TABLE cris_rp_box2containable RENAME TO cris_rp_box2con;
ALTER TABLE cris_rp_edittab RENAME TO cris_rp_etab;
ALTER TABLE cris_rp_edittab2box RENAME TO cris_rp_etab2box;
ALTER TABLE cris_rp_etab2box RENAME COLUMN cris_rp_edittab_id to cris_rp_etab_id;


ALTER TABLE cris_rp_nestedobject RENAME TO cris_rp_no;
ALTER TABLE cris_rp_no DROP CONSTRAINT cris_rp_nestedobject_position_typo_id_parent_id_key;
ALTER TABLE cris_rp_no RENAME COLUMN position to positionDef;
alter table cris_rp_no ADD COLUMN scopeDef_id int4;
ALTER TABLE cris_rp_no ADD CONSTRAINT cris_rp_no_positiondef_typo_id_parent_id_key UNIQUE (positionDef, typo_id, parent_id);
alter table cris_rp_no add constraint FKC3CB15F7A7AEA5D2863f697c foreign key (scopeDef_id) references jdyna_scopedefinition;

ALTER TABLE cris_rp_nestedobject_propertiesdefinition RENAME TO cris_rp_no_pdef;
ALTER TABLE cris_rp_nestedobject_prop RENAME TO cris_rp_no_prop;
ALTER TABLE cris_rp_no_prop DROP CONSTRAINT cris_rp_nestedobject_prop_position_typo_id_parent_id_key;
ALTER TABLE cris_rp_no_prop ADD endDate timestamp;
ALTER TABLE cris_rp_no_prop ADD startDate timestamp;
ALTER TABLE cris_rp_no_prop RENAME COLUMN lock to lockDef;
ALTER TABLE cris_rp_no_prop RENAME COLUMN position to positionDef;
alter table cris_rp_no_prop ADD COLUMN scopeDef_id int4;
alter table cris_rp_no_prop DROP COLUMN scope;
ALTER TABLE cris_rp_no_prop ADD CONSTRAINT cris_rp_no_prop_positiondef_typo_id_parent_id_key UNIQUE (positionDef, typo_id, parent_id);
alter table cris_rp_no_prop add constraint FKC8A841F5A7AEA5D28f028046 foreign key (scopeDef_id) references jdyna_scopedefinition;

ALTER TABLE cris_rp_nestedobject_typo RENAME TO cris_rp_no_tp;
ALTER TABLE cris_rp_nestedobject_typo2mask RENAME TO cris_rp_no_tp2pdef;
ALTER TABLE cris_rp_no_tp2pdef RENAME COLUMN cris_rp_nestedobject_typo_id to cris_rp_no_tp_id;

ALTER TABLE cris_rp_propertiesdefinition RENAME TO cris_rp_pdef;

alter table cris_rp_prop add column endDate timestamp;
alter table cris_rp_prop add column startDate timestamp;
alter table cris_rp_prop RENAME COLUMN lock to lockDef;
alter table cris_rp_prop RENAME COLUMN position to positionDef;
alter table cris_rp_prop ADD COLUMN scopeDef_id int4;
alter table cris_rp_prop DROP COLUMN scope;
alter table cris_rp_prop add constraint FKC8A841F5A7AEA5D2f40bfc5e foreign key (scopeDef_id) references jdyna_scopedefinition;

ALTER TABLE cris_rp_widgetfile RENAME TO cris_rp_wfile;
ALTER TABLE cris_rp_wfile RENAME COLUMN size to widgetSize;

ALTER TABLE cris_webservice_criteriaws RENAME TO cris_ws_criteria;
ALTER TABLE cris_webservice_userws RENAME TO cris_ws_user;
ALTER TABLE cris_ws_user RENAME COLUMN type to typeDef;
ALTER TABLE cris_webservice_user2criteria RENAME TO cris_ws_user2crit;
ALTER TABLE cris_ws_user2crit RENAME COLUMN cris_webservice_userws_id to cris_ws_user_id;

DROP TABLE jdyna_nestedobject CASCADE;
DROP TABLE jdyna_nestedobject_prop;
DROP TABLE jdyna_nestedobject_propertiesdefinition CASCADE;
DROP TABLE jdyna_nestedobject_typo2mask;
DROP TABLE jdyna_nestedobject_typo;

create table jdyna_no (id int4 not null, endDate timestamp, startDate timestamp, positionDef int4, status bool, timestampCreated timestamp, timestampLastModified timestamp, uuid varchar(255) not null unique, scopeDef_id int4, typo_id int4, primary key (id));
create table jdyna_no_pdef (id int4 not null, accessLevel int4, advancedSearch bool not null, fieldmin_col int4, fieldmin_col_unit varchar(255), fieldmin_row_unit varchar(255), fieldmin_row int4, help text, label varchar(255), labelMinSize int4 not null, labelMinSizeUnit varchar(255), mandatory bool not null, newline bool not null, onCreation bool, priority int4 not null, repeatable bool not null, shortName varchar(255) unique, showInList bool not null, simpleSearch bool not null, rendering_id int4 unique, primary key (id));
create table jdyna_no_prop (id int4 not null, endDate timestamp, startDate timestamp, lockDef int4, positionDef int4 not null, visibility int4, scopeDef_id int4, value_id int4 unique, parent_id int4, typo_id int4, primary key (id), unique (positionDef, typo_id, parent_id));
create table jdyna_no_tp (id int4 not null, label varchar(255), shortName varchar(255), accessLevel int4, help text, inline bool not null, mandatory bool not null, newline bool not null, priority int4 not null, repeatable bool not null, primary key (id));
create table jdyna_no_tp2pdef (jdyna_no_tp_id int4 not null, mask_id int4 not null);
alter table jdyna_no add constraint FKC3CB15F7A7AEA5D281e1c0ae foreign key (scopeDef_id) references jdyna_scopedefinition;
alter table jdyna_no add constraint FK81E1C0AE47739B84 foreign key (typo_id) references jdyna_no_tp;
alter table jdyna_no_prop add constraint FKC8A841F5A7AEA5D25390d854 foreign key (scopeDef_id) references jdyna_scopedefinition;
alter table jdyna_no_prop add constraint FK5390D85499D0511 foreign key (typo_id) references jdyna_no_pdef;
alter table jdyna_no_prop add constraint FKC8A841F5E52079D75390d854 foreign key (value_id) references jdyna_values;
alter table jdyna_no_prop add constraint FK5390D8549B08C304 foreign key (parent_id) references jdyna_no;
alter table jdyna_no_tp2pdef add constraint FK719B7D3A99099A7B foreign key (jdyna_no_tp_id) references jdyna_no_tp;
alter table jdyna_no_tp2pdef add constraint FK719B7D3A6E858C69 foreign key (mask_id) references jdyna_no_pdef;
create index jdyna_no_pprop_idx on jdyna_no_prop (parent_id);

alter table jdyna_widget_link rename column size to widgetSize;
alter table jdyna_widget_pointer rename column size to widgetSize;
alter table jdyna_widget_text rename column col to widgetcol;
alter table jdyna_widget_text rename column row to widgetrow;


ALTER INDEX public.cris_ou_nestedobject_prop_parent_id RENAME TO cris_ou_no_pprop_idx;
ALTER INDEX public.cris_ou_prop_parent_id RENAME TO cris_ou_pprop_idx;
ALTER INDEX public.cris_project_nestedobject_prop_parent_id RENAME TO cris_pj_no_pprop_idx;
ALTER INDEX public.cris_project_prop_idx_parent_id RENAME TO cris_pj_pprop_idx;
ALTER INDEX public.cris_rp_nestedobject_prop_parent_id RENAME TO cris_rp_no_pprop_idx;
ALTER INDEX public.cris_rp_prop_parent_id RENAME TO cris_rp_pprop_idx;

ALTER TABLE jdyna_containables RENAME TO jdyna_containable;  
alter table jdyna_containable RENAME COLUMN propertiesdefinitionou_fk to cris_ou_pdef_fk;
alter table jdyna_containable RENAME COLUMN propertiesdefinition_fk to cris_rp_pdef_fk;
alter table jdyna_containable RENAME COLUMN propertiesdefinitionproject_fk to cris_pj_pdef_fk;


alter table jdyna_containable RENAME COLUMN propertiesdefinitionnestedobject_fk to jdyna_no_pdef_fk;
alter table jdyna_containable RENAME COLUMN pdounestedobject_fk to cris_ou_no_pdef_fk;
alter table jdyna_containable RENAME COLUMN pdprojectnestedobject_fk to cris_pj_no_pdef_fk;
alter table jdyna_containable RENAME COLUMN pdrpnestedobject_fk to cris_rp_no_pdef_fk;

alter table jdyna_containable RENAME COLUMN typeounestedobject_fk to cris_ou_no_tp_fk;
alter table jdyna_containable RENAME COLUMN typerpnestedobject_fk to cris_rp_no_tp_fk;
alter table jdyna_containable RENAME COLUMN typeprojectnestedobject_fk to cris_pj_no_tp_fk;

ALTER INDEX public.jdyna_values_idx_dtype RENAME TO jdyna_values_dtype_idx;

ALTER TABLE CRIS_CRITERIAWS_SEQ RENAME TO CRIS_WS_CRITERIA_SEQ;
ALTER TABLE CRIS_USERWS_SEQ RENAME TO CRIS_WS_USER_SEQ;
ALTER TABLE BOX_SEQ RENAME TO JDYNA_BOX_SEQ;
ALTER TABLE CONTAINABLE_SEQ RENAME TO JDYNA_CONTAINABLE_SEQ;
ALTER TABLE PROPERTIESDEFINITION_SEQ RENAME TO JDYNA_PDEF_SEQ;
ALTER TABLE PROPERTY_SEQ RENAME TO JDYNA_PROP_SEQ;
ALTER TABLE TAB_SEQ RENAME TO JDYNA_TAB_SEQ;
ALTER TABLE VALUES_SEQ RENAME TO JDYNA_VALUES_SEQ;
ALTER TABLE WIDGET_SEQ RENAME TO JDYNA_WIDGET_SEQ;
ALTER TABLE CRIS_RELATIONPREFERENCE_SEQ RENAME TO CRIS_RELPREF_SEQ;
ALTER TABLE CRIS_RESEARCHERPAGE_SEQ RENAME TO CRIS_RPAGE_SEQ;

ALTER TABLE cris_researcherpage RENAME TO cris_rpage;
ALTER TABLE cris_relationpreference RENAME TO cris_relpref;

create sequence JDYNA_SCOPEDEF_SEQ;

alter table cris_ou_box2con rename column mask_id to jdyna_containable_id;
alter table cris_ou_no_tp2pdef rename column mask_id to cris_ou_no_pdef_id;
alter table cris_ou_tab2box rename column mask_id to cris_ou_box_id;
alter table cris_pj_box2con rename column mask_id to jdyna_containable_id;
alter table cris_pj_no_tp2pdef  rename column mask_id to cris_pj_no_pdef_id;


alter table cris_pj_tab2box rename column mask_id to cris_pj_box_id;
alter table cris_rp_tab2box rename column mask_id to cris_rp_box_id;

alter table cris_rp_box2con rename column mask_id to jdyna_containable_id;

alter table cris_rp_no_tp2pdef  rename column mask_id to cris_rp_no_pdef_id;
alter table jdyna_no_tp2pdef  rename column mask_id to jdyna_no_pdef_id;

--substring e rename
update jdyna_values set filevalue = substring(filevalue, 0, 254) where filevalue is not null;
alter table jdyna_values rename column filevalue to filename;
alter table jdyna_values ALTER filename TYPE varchar(255);

update jdyna_values set linkvalue = substring(linkvalue, 0, 254) where linkvalue is not null;
alter table jdyna_values ALTER linkvalue TYPE varchar(255);

alter table cris_rp_etab2box rename column mask_id to cris_rp_box_id;
alter table cris_pj_etab2box rename column mask_id to cris_pj_box_id;
alter table cris_ou_etab2box rename column mask_id to cris_ou_box_id;

ALTER TABLE jdyna_values RENAME testovalue  TO textvalue;
update jdyna_values  set dtype  = 'text' where textvalue is not null;

alter table jdyna_box_message drop column elementAfter;
alter table jdyna_box_message add column elementAfter_id int4;
alter table jdyna_values add column doubleValue float8;
alter table jdyna_box_message add constraint FK74D7714673538EAA foreign key (elementAfter_id) references jdyna_containable;
create table jdyna_widget_number (id int4 not null, max float8, min float8, precisionDef int4 not null, widgetSize int4, primary key (id));