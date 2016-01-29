--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin
	
	ALTER TABLE cris_metrics ADD COLUMN last boolean;
	update cris_metrics set last = true where id in (select max(id) from cris_metrics group by resourceid, resourcetypeid, metrictype);
	
	CREATE INDEX metrics_uuid_idx ON cris_metrics (uuid ASC NULLS LAST);
	CREATE INDEX metric_resourceuid_idx ON cris_metrics (resourceid ASC NULLS LAST, resourcetypeid ASC NULLS LAST);
    CREATE INDEX metric_bid_idx ON cris_metrics (resourceid ASC NULLS LAST, resourcetypeid ASC NULLS LAST, metrictype ASC NULLS LAST);
	CREATE INDEX metrics_last_idx ON cris_metrics (last ASC NULLS LAST);

exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
