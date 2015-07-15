ALTER TABLE jdyna_values ADD COLUMN booleanValue bool;
create table IF NOT EXISTS jdyna_widget_boolean (id int4 not null, showAsType varchar(255), checked bool, hideWhenFalse bool, primary key (id));
create table IF NOT EXISTS jdyna_widget_checkradio (id int4 not null, option4row integer, staticValues text, dropdown integer, primary key (id));
--ALTER TABLE jdyna_widget_checkradio DROP COLUMN query;

-- Table to mantain the potential match between item and rp --
CREATE TABLE IF NOT EXISTS potentialmatches
(
   potentialmatches_id integer, 
   item_id integer, 
   rp character varying(20), 
   pending integer,
    PRIMARY KEY (potentialmatches_id)
);
CREATE SEQUENCE potentialmatches_seq;
CREATE INDEX rp_idx
   ON potentialmatches (rp ASC NULLS LAST);
CREATE INDEX pending_idx
   ON potentialmatches (pending);
-- END potential matches --
   
   
create table IF NOT EXISTS cris_orcid_history (id int4 not null, entityId int4, typeId int4, responseMessage text, lastAttempt timestamp, lastSuccess timestamp, primary key (id));
create table IF NOT EXISTS cris_orcid_queue (id int4 not null, owner varchar(255), entityId int4, typeId int4, mode varchar(255), primary key (id));
create sequence CRIS_ORCIDHISTORY_SEQ;
create sequence CRIS_ORCIDQUEUE_SEQ;   
--ALTER TABLE cris_orcid_history ADD COLUMN entityId integer;
--ALTER TABLE cris_orcid_history ADD COLUMN typeId integer;
--ALTER TABLE cris_orcid_queue ADD COLUMN entityId integer;
--ALTER TABLE cris_orcid_queue ADD COLUMN typeId integer;
--
--ALTER TABLE cris_orcid_queue DROP COLUMN itemId;
--ALTER TABLE cris_orcid_queue DROP COLUMN projectId;
--ALTER TABLE cris_orcid_queue DROP COLUMN researcherId;
--ALTER TABLE cris_orcid_queue DROP COLUMN send;
--
--ALTER TABLE cris_orcid_history DROP COLUMN itemId;
--ALTER TABLE cris_orcid_history DROP COLUMN projectId;
--ALTER TABLE cris_orcid_history DROP COLUMN researcherId;