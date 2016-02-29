/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.common.dao;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.hibernate.Query;

public class MetricsApplicationDao
        extends it.cilea.osd.common.dao.impl.ApplicationDao
{

    private final String query = "select cm1.uuid as uuid, subq.resourceid as resourceid, subq.resourcetypeid as resourcetypeid, subq.limitsx as startdate, subq.limitdx as enddate, case when (cm2.metriccount - cm1.metriccount) < 0 then 0 else (cm2.metriccount - cm1.metriccount) end as metriccount"
            + " from"
            + " (select cm1.resourceid, cm1.resourcetypeid, max(cm1.enddate) as limitsx, min(cm2.enddate) as limitdx, cm1.metrictype"
            + " from cris_metrics cm1 join cris_metrics cm2"
            + " on cm1.resourceid = cm2.resourceid and cm1.metrictype = cm2.metrictype and cm1.resourcetypeid = cm2.resourcetypeid"
            + " where cm1.metricType = ?" + " and cm1.enddate < ?"
            + " and cm2.enddate > ?"
            + " group by cm1.resourceid, cm1.resourcetypeid, cm1.metrictype) subq"
            + " join cris_metrics cm1"
            + " on cm1.resourceid = subq.resourceid and subq.metrictype = cm1.metrictype and cm1.resourcetypeid = subq.resourcetypeid and subq.limitsx = cm1.enddate"
            + " join cris_metrics cm2"
            + " on cm2.resourceid = subq.resourceid and subq.metrictype = cm2.metrictype and cm2.resourcetypeid = subq.resourcetypeid and subq.limitdx = cm2.enddate";

    private final String queryUpdateLast = "update cris_metrics set last = false where metrictype = ? and last = true and resourcetypeid = ? and resourceid = ? and timestampcreated < ?";
    
    public void buildPeriodMetrics(Context context, String suffixNewType,
            String type, long rangeLimitSx, long rangeLimitDx)
                    throws SQLException
    {

        TableRowIterator tri = null;
        try
        {
            tri = DatabaseManager.query(context, query, type,
                    new Timestamp(rangeLimitSx), new Timestamp(rangeLimitDx));
            while (tri.hasNext())
            {
                TableRow rowSelect = tri.next();
                TableRow row = DatabaseManager.row("cris_metrics");
                
                String metrictype = type + suffixNewType;
                Date currentTimestamp = new Date();
                int resourceId = rowSelect.getIntColumn("resourceid");
                int resourceTypeId = rowSelect.getIntColumn("resourcetypeid");
                //just set false the column last
                DatabaseManager.updateQuery(context, queryUpdateLast, metrictype, resourceTypeId, resourceId, new Timestamp(currentTimestamp.getTime()));
                
                //build the new one
                row.setColumn("timestampcreated", currentTimestamp);
                row.setColumn("timestampLastModified", currentTimestamp);
                
                row.setColumn("startdate", new Timestamp(
                        rowSelect.getDateColumn("startdate").getTime()));
                row.setColumn("enddate", new Timestamp(
                        rowSelect.getDateColumn("enddate").getTime()));
                row.setColumn("metriccount",
                        rowSelect.getDoubleColumn("metriccount"));
                
                row.setColumn("resourceid",
                        resourceId);                
                row.setColumn("resourcetypeid",
                        resourceTypeId);
                row.setColumn("uuid", rowSelect.getStringColumn("uuid"));
                
                row.setColumn("metrictype", metrictype);
                row.setColumn("last", true);
                
                DatabaseManager.insert(context, row);
            }
        }
        finally
        {
            if (tri != null)
            {
                tri.close();
            }
        }
    }

    /**
     * 
     * @param resourceTypeId
     * @param resourceId
     * @param metricType
     * @return the number of updated metrics
     */
	public int unsetLastMetric(Integer resourceTypeId, Integer resourceId, String metricType) {
        Query query = getSessionFactory().getCurrentSession().createQuery(
                "update org.dspace.app.cris.metrics.common.model.CrisMetrics set last = false where resourceTypeId = ? and resourceId = ? and metricType = ?");
        
        query.setParameter(0, resourceTypeId);
        query.setParameter(1, resourceId);
        query.setParameter(2, metricType);

        return query.executeUpdate();
	}

}
