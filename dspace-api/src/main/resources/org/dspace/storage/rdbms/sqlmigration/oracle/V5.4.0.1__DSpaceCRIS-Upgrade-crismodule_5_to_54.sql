--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE
    	'ALTER TABLE cris_metrics ADD COLUMN last number(1,0) not null';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'update cris_metrics set last = true where id in (select max(id) from cris_metrics group by resourceid, resourcetypeid, metrictype)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE INDEX metrics_uuid_idx ON cris_metrics (uuid ASC)';
    EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE INDEX metric_resourceuid_idx ON cris_metrics (resourceid ASC, resourcetypeid ASC)';    	
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE INDEX metric_bid_idx ON cris_metrics (resourceid ASC, resourcetypeid ASC, metrictype ASC)';
    EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE INDEX metrics_last_idx ON cris_metrics (last ASC)';		 
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;
