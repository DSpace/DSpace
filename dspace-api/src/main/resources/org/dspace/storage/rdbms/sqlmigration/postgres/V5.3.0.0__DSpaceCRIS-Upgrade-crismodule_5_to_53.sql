--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE jdyna_values ADD COLUMN booleanValue bool;
create table IF NOT EXISTS jdyna_widget_boolean (id int4 not null, showAsType varchar(255), checked bool, hideWhenFalse bool, primary key (id));


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

   
create table cris_orcid_history (id int4 not null, itemId int4, projectId int4, researcherId int4, responseMessage text, lastAttempt timestamp, lastSuccess timestamp, primary key (id));
create table cris_orcid_queue (id int4 not null, itemId int4, mode varchar(255), projectId int4, researcherId int4, send boolean not null, primary key (id));
create sequence CRIS_ORCIDHISTORY_SEQ;
create sequence CRIS_ORCIDQUEUE_SEQ;