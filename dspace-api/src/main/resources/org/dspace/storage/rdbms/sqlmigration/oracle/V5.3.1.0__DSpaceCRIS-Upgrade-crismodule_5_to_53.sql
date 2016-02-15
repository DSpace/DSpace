--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE
    'create table cris_metrics
	(
		id number(10,0) not null,
		metricCount double precision not null,
		endDate timestamp,
		remark clob,
		resourceId number(10,0),
		resourceTypeId number(10,0),
		startDate timestamp,
		timestampCreated timestamp,
		timestampLastModified timestamp,
		metricType varchar2(255 char),
		uuid varchar2(255 char),
		primary key (id)
    )';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'create sequence CRIS_METRICS_SEQ';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;