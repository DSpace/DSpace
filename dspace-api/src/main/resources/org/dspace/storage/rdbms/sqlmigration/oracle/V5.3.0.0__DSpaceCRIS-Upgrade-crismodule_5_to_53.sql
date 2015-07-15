--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE jdyna_values ADD booleanValue number(1,0);
create table IF NOT EXISTS jdyna_widget_boolean (id number(10,0) not null, showAsType varchar2(255), checked number(1,0), hideWhenFalse number(1,0), primary key (id));

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

create table cris_orcid_history (id number(10,0) not null, itemId number(10,0), projectId number(10,0), researcherId number(10,0), responseMessage clob, lastAttempt timestamp, lastSuccess timestamp, primary key (id));
create table cris_orcid_queue (id number(10,0) not null, itemId number(10,0), mode varchar2(255), projectId number(10,0), researcherId number(10,0), send number(1,0) not null, primary key (id));
create sequence CRIS_ORCIDHISTORY_SEQ;
create sequence CRIS_ORCIDQUEUE_SEQ;