--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

create table IF NOT EXISTS cris_orcid_history (id number(10,0) not null, owner varchar2(255), entityId number(10,0), typeId number(10,0), responseMessage clob, lastAttempt timestamp, lastSuccess timestamp, primary key (id));
create table IF NOT EXISTS cris_orcid_queue (id number(10,0) not null, owner varchar2(255), entityId number(10,0), typeId number(10,0), mode varchar2(255), fastlookupobjectname clob, fastlookupuuid varchar2(255), primary key (id));

create table IF NOT EXISTS jdyna_widget_checkradio (id number(10,0) not null, option4row number(10,0), staticValues text, dropdown number(1,0), primary key (id));
alter table jdyna_widget_checkradio drop column IF EXISTS query;

BEGIN
	EXECUTE IMMEDIATE
    	'create sequence CRIS_ORCIDHISTORY_SEQ';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'create sequence CRIS_ORCIDQUEUE_SEQ';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'alter table jdyna_widget_checkradio add column dropdown number(1,0)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;