/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metrics.dao;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface CrisMetricsDAO extends GenericDAO<CrisMetrics> {

    public List<CrisMetrics> findAll(Context context, Integer limit, Integer offset) throws SQLException;

    public List<CrisMetrics> findAllByItem(Context context, Item item) throws SQLException;

    public List<CrisMetrics> findAllLast(Context context, Integer limit, Integer offset) throws SQLException;

    public int countAllLast(Context context) throws SQLException;

    public int countRows(Context context) throws SQLException;

    public void delete(Context context, CrisMetrics crisMetrics) throws SQLException;

    public CrisMetrics findLastMetricByResourceIdAndMetricsTypes(Context context, String metricType, UUID resourceId)
           throws SQLException;

    public CrisMetrics uniqueLastMetricByResourceIdAndResourceTypeIdAndMetricsType(Context context, String metricType,
            UUID resource, boolean last) throws SQLException;

}