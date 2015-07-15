ALTER TABLE jdyna_values ADD booleanValue number(1,0);
create table IF NOT EXISTS jdyna_widget_boolean (id number(10,0) not null, showAsType varchar2(255), checked number(1,0), hideWhenFalse number(1,0), primary key (id));
create table IF NOT EXISTS jdyna_widget_checkradio (id number(10,0) not null, option4row number(10,0), staticValues text, dropdown number(1,0), primary key (id));
--ALTER TABLE jdyna_widget_checkradio DROP COLUMN query;

-- Table to mantain the potential match between item and rp --
CREATE TABLE IF NOT EXISTS potentialmatches
(
   potentialmatches_id integer, 
   item_id integer, 
   rp VARCHAR2(20 BYTE),
   pending number(1),
    PRIMARY KEY (potentialmatches_id)
);
CREATE SEQUENCE potentialmatches_seq;
CREATE INDEX rp_idx ON potentialmatches (rp);
CREATE INDEX pending_idx ON potentialmatches (pending);
-- END potential matches --

create table IF NOT EXISTS cris_orcid_history (id number(10,0) not null, entityId number(10,0), typeId number(10,0), responseMessage clob, lastAttempt timestamp, lastSuccess timestamp, primary key (id));
create table IF NOT EXISTS cris_orcid_queue (id number(10,0) not null, owner varchar2(255), entityId number(10,0), typeId number(10,0), mode varchar2(255), primary key (id));
create sequence CRIS_ORCIDHISTORY_SEQ;
create sequence CRIS_ORCIDQUEUE_SEQ;

--ALTER TABLE cris_orcid_history ADD COLUMN entityId number(1,0);
--ALTER TABLE cris_orcid_history ADD COLUMN typeId number(1,0);
--ALTER TABLE cris_orcid_queue ADD COLUMN entityId number(1,0);
--ALTER TABLE cris_orcid_queue ADD COLUMN typeId number(1,0);
--
--ALTER TABLE cris_orcid_queue DROP COLUMN itemId;
--ALTER TABLE cris_orcid_queue DROP COLUMN projectId;
--ALTER TABLE cris_orcid_queue DROP COLUMN researcherId;
--ALTER TABLE cris_orcid_queue DROP COLUMN send;

--ALTER TABLE cris_orcid_history DROP COLUMN itemId;
--ALTER TABLE cris_orcid_history DROP COLUMN projectId;
--ALTER TABLE cris_orcid_history DROP COLUMN researcherId;