/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import static java.util.Optional.ofNullable;
import static org.dspace.util.UUIDUtils.fromString;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.statistics.SolrLoggerServiceImpl.StatisticsType;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;
import org.dspace.statistics.service.LoginStatisticsService;
import org.dspace.statistics.service.SolrLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link LoginStatisticsService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class LoginStatisticsServiceImpl implements LoginStatisticsService {

    @Autowired
    private SolrLoggerService solrLoggerService;

    @Autowired
    private EPersonService ePersonService;

    @Override
    public Optional<LoginStatistics> find(Context context, String id) {

        String filterQuery = "id:" + id + " AND " + getLoginTypeFilter();
        ObjectCount[] objectCounts = queryFacetField(filterQuery, 1);

        if (objectCounts.length == 0) {
            return Optional.empty();
        }

        return buildLoginStatistics(context, objectCounts[0]);
    }

    @Override
    public List<LoginStatistics> findByDateRange(Context context, Date startDate, Date endDate, int limit) {

        String filterQuery = calculateFilterQuery(startDate, endDate);

        ObjectCount[] objectCounts = queryFacetField(filterQuery, limit);

        List<LoginStatistics> statistics = new ArrayList<>();

        for (int i = 0; i < objectCounts.length; i++) {
            buildLoginStatistics(context, objectCounts[i]).ifPresent(statistics::add);
        }

        return statistics;
    }

    private String calculateFilterQuery(Date startDate, Date endDate) {
        return getDateFilter(startDate, endDate)
            .map(dateFilter -> dateFilter + " AND " + getLoginTypeFilter())
            .orElseGet(() -> getLoginTypeFilter());
    }

    private Optional<EPerson> findUserByUuid(Context context, String uuid) {
        try {
            return ofNullable(ePersonService.find(context, fromString(uuid)));
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private Optional<LoginStatistics> buildLoginStatistics(Context context, ObjectCount objectCount) {
        if (objectCount.getCount() == 0L) {
            return Optional.empty();
        }

        return findUserByUuid(context, objectCount.getValue())
            .map(user -> new LoginStatistics(user, objectCount.getCount()));
    }

    private Optional<String> getDateFilter(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) {
            return Optional.empty();
        }

        StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
        dateFilter.setStartDate(
            startDate != null
                ? LocalDateTime.ofInstant(startDate.toInstant(), ZoneId.systemDefault())
                : LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)
        );
        dateFilter.setEndDate(
            endDate != null
                ? LocalDateTime.ofInstant(endDate.toInstant(), ZoneId.systemDefault())
                : LocalDateTime.now(ZoneId.systemDefault())
        );
        return Optional.of(dateFilter.toQuery());
    }

    private String getLoginTypeFilter() {
        return "statistics_type:" + StatisticsType.LOGIN.text();
    }

    private ObjectCount[] queryFacetField(String filter, int limit) {
        try {
            return solrLoggerService.queryFacetField("*:*", filter, "epersonid", limit, false, null, 0);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
