/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metrics.service;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface CrisMetricsService {

    public List<CrisMetrics> findAll(Context context) throws SQLException;

    public List<CrisMetrics> findAll(Context context, Integer limit, Integer offset) throws SQLException;

    public int count(Context context) throws SQLException;

    public CrisMetrics create(Context context, Item item) throws SQLException, AuthorizeException;

    public void delete(Context context, CrisMetrics crisMetrics) throws SQLException, AuthorizeException;

    public CrisMetrics findLastMetricByResourceIdAndMetricsTypes(Context context, String metricType, UUID resourceUuid)
           throws SQLException;

    public CrisMetrics uniqueLastMetricByResourceIdAndResourceTypeIdAndMetricsType(Context context, String metricType,
           UUID resourceUuid, boolean last) throws SQLException;

    public void update(Context context, CrisMetrics crisMetrics) throws SQLException, AuthorizeException;

    public CrisMetrics find(Context context, int id) throws SQLException;
}