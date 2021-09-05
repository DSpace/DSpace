/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metrics.service;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * Service interface class for the CrisMetrics object.
 * This interface defines the contract of the service that is responsible
 * for all the business logic calls for the cris metrics object.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface CrisMetricsService {

    public List<CrisMetrics> findAll(Context context) throws SQLException;

    public List<CrisMetrics> findAll(Context context, Integer limit, Integer offset) throws SQLException;

    public List<CrisMetrics> findAllByDSO(Context context, DSpaceObject dSpaceObject) throws SQLException;

    public List<CrisMetrics> findAllLast(Context context, Integer limit, Integer offset) throws SQLException;

    public int countAllLast(Context context) throws SQLException;

    public int count(Context context) throws SQLException;

    public CrisMetrics create(Context context, DSpaceObject dSpaceObject) throws SQLException, AuthorizeException;

    public void delete(Context context, CrisMetrics crisMetrics) throws SQLException, AuthorizeException;

    public void deleteByResourceID(Context context, DSpaceObject dSpaceObject) throws SQLException, AuthorizeException;

    public CrisMetrics findLastMetricByResourceIdAndMetricsTypes(Context context, String metricType, UUID resourceUuid)
            throws SQLException;

    public CrisMetrics uniqueLastMetricByResourceIdAndResourceTypeIdAndMetricsType(Context context, String metricType,
                             UUID resourceUuid, boolean last) throws SQLException;

    public void update(Context context, CrisMetrics crisMetrics) throws SQLException, AuthorizeException;

    public CrisMetrics find(Context context, int id) throws SQLException;

    /**
     * Search a CrisMetric for a certain period like [week or month] from a certain startDate
     *
     * @param context         DSpace context object
     * @param metricType      the CrisMetric type
     * @param resourceId      the uuid of an DSpace resource
     * @param startDate       date from which the period is to be extended
     * @param period          period can be either a week or a month [week or month].
     * @return
     * @throws SQLException   if database error
     */
    public Optional<CrisMetrics> getCrisMetricByPeriod(Context context,
                                                       String metricType, UUID resourceId,
                                                       Date startDate, String period) throws SQLException;

}
