/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.statistics;

import static java.time.ZoneOffset.UTC;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.rest.model.UsageReportPointDateRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.dspace.statistics.content.StatisticsDatasetDisplay;
import org.dspace.statistics.service.SolrLoggerService;
import org.dspace.util.MultiFormatDateParser;
import org.dspace.util.SolrUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This report generator provides usage data aggregated over a specific period
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class TotalVisitPerPeriodGenerator extends AbstractUsageReportGenerator {

    private static final String POINT_VIEWS_KEY = "views";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private SolrLoggerService solrLoggerService;

    @Autowired
    private DiscoveryConfigurationService discoveryConfigurationService;

    private String periodType = "month";

    private int increment = 1;

    private String defaultStartDate;

    private Integer dsoType;

    /**
     * Create a stat usage report for the amount of TotalVisitPerMonth on a DSO, containing one point for each month
     * with the views on that DSO in that month with the range -6 months to now. If there are no views on the DSO
     * in a month, the point on that month contains views=0.
     *
     * @param context   DSpace context
     * @param dso       DSO we want usage report with TotalVisitsPerMonth to the DSO
     * @param startDate String to filter the start date of statistic
     * @param endDate   String to filter the end date of statistic
     * @return Rest object containing the TotalVisits usage report on the given DSO
     */
    public UsageReportRest createUsageReport(Context context, DSpaceObject dso, String startDate, String endDate)
        throws IOException, SolrServerException {

        boolean hasValidRelation = false;
        StatisticsDatasetDisplay statisticsDatasetDisplay = new StatisticsDatasetDisplay();
        StringBuilder query = new StringBuilder();

        if (getRelation() != null) {
            DiscoveryConfiguration discoveryConfiguration = discoveryConfigurationService
                .getDiscoveryConfigurationByName(getRelation());
            if (discoveryConfiguration == null) {
                // not valid because not found bean with this relation configuration name
                hasValidRelation = false;
            } else {
                hasValidRelation = true;
                query.append(
                    statisticsDatasetDisplay
                        .composeQueryWithInverseRelation(
                            dso,
                            discoveryConfiguration.getDefaultFilterQueries(),
                            getDsoType(dso)
                        )
                );
            }
        }

        if (!hasValidRelation) {
            if (getDsoType(dso) != -1) {
                query.append("type: ").append(getDsoType(dso));
            }

            if (isNotSiteObject(dso)) {
                query.append((query.length() == 0 ? "" : " AND "))
                     .append("id:").append(dso.getID());
            }
        }

        boolean startDateProvided = !isBlankDate(startDate);

        startDate = isBlankDate(startDate) ? getDefaultStartDate() : startDate;
        endDate = isBlankDate(endDate) ? formatDate(LocalDate.now()) : endDate;

        validateStartAndEndDates(startDate, endDate);

        if (isBlank(startDate) || isAfterToday(startDate)) {
            return buildEmptyDataReport(context, endDate);
        }

        String rangeStart = calculateRangeStart(startDate);
        String rangeEnd = calculateRangeEnd(endDate);

        String filterQuery = statisticsDatasetDisplay
            .composeFilterQuery(startDate, endDate, hasValidRelation, getDsoType(dso));

        // execute query
        ObjectCount[] dateFacetResult = solrLoggerService.queryFacetDateField(context, "id", null, query.toString(),
                                                                              filterQuery, periodType.toUpperCase(),
                                                                              rangeStart, rangeEnd, false, 0,
                                                                              increment);

        List<UsageReportPointDateRest> usageReportPoints = convertToReportPoints(dateFacetResult, startDateProvided);

        if (CollectionUtils.isEmpty(usageReportPoints)) {
            return buildEmptyDataReport(context, endDate);
        }

        UsageReportRest usageReportRest = new UsageReportRest();
        usageReportPoints.forEach(usageReportRest::addPoint);

        return usageReportRest;
    }

    private String calculateRangeStart(String startDate) {

        long period = calculatePeriodFromToday(startDate);

        if (period == 0L) {
            return "";
        }

        return "-" + period;

    }

    private String calculateRangeEnd(String endDate) {

        long period = calculatePeriodFromToday(endDate);

        if (period == 0L || isAfterToday(endDate)) {
            return "+1";
        }

        return "-" + (--period);

    }

    private long calculatePeriodFromToday(String date) {
        LocalDate startDate = MultiFormatDateParser.parse(date).toLocalDate();
        LocalDate endDate = LocalDate.now();
        return calculatePeriodBetweenDates(startDate, endDate);
    }

    private long calculatePeriodBetweenDates(LocalDate startDate, LocalDate endDate) {

        ChronoUnit chronoUnit = getConfiguredChronoUnit();

        if (chronoUnit == ChronoUnit.MONTHS) {
            startDate = startDate.with(ChronoField.DAY_OF_MONTH, 1L);
            endDate = endDate.with(ChronoField.DAY_OF_MONTH, 1L);
        } else if (chronoUnit == ChronoUnit.YEARS) {
            startDate = startDate.with(ChronoField.DAY_OF_YEAR, 1L);
            endDate = endDate.with(ChronoField.DAY_OF_YEAR, 1L);
        }

        return chronoUnit.between(startDate, endDate);
    }

    private ChronoUnit getConfiguredChronoUnit() {
        switch (periodType.toUpperCase()) {
            case "MONTH":
                return ChronoUnit.MONTHS;
            case "YEAR":
                return ChronoUnit.YEARS;
            case "DAY":
                return ChronoUnit.DAYS;
            default:
                throw new IllegalStateException("Unsupported period type " + periodType);
        }
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant()
                   .atZone(ZoneId.systemDefault())
                   .toLocalDate();
    }

    private List<UsageReportPointDateRest> convertToReportPoints(ObjectCount[] countResult, boolean startDateProvided) {

        List<UsageReportPointDateRest> points = new ArrayList<>();
        boolean pointWithDataFound = false;

        for (ObjectCount dateCount : countResult) {
            UsageReportPointDateRest reportPoint = convertToUsageReportPoint(dateCount);
            pointWithDataFound = pointWithDataFound || dateCount.getCount() > 0;
            if (startDateProvided || pointWithDataFound) {
                points.add(reportPoint);
            }
        }

        return points;
    }

    private UsageReportPointDateRest convertToUsageReportPoint(ObjectCount objectCount) {
        UsageReportPointDateRest point = new UsageReportPointDateRest();
        point.setId(objectCount.getValue());
        point.addValue(POINT_VIEWS_KEY, (int) objectCount.getCount());
        return point;
    }

    private UsageReportRest buildEmptyDataReport(Context context, String endDate) {
        UsageReportRest usageReportRest = new UsageReportRest();

        String format = SolrUtils.getDateformatFrom(periodType.toUpperCase());
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format, context.getCurrentLocale());

        UsageReportPointDateRest point = new UsageReportPointDateRest();
        point.addValue(POINT_VIEWS_KEY, 0);

        ZonedDateTime zonedDateTime = MultiFormatDateParser.parse(endDate);
        if (zonedDateTime == null) {
            throw new IllegalArgumentException("Invalid endDate: " + endDate);
        }

        point.setId(zonedDateTime.format(dateTimeFormatter));
        usageReportRest.addPoint(point);

        return usageReportRest;
    }

    private String getDefaultStartDate() throws SolrServerException, IOException {

        if (StringUtils.isNotBlank(defaultStartDate)) {
            return defaultStartDate;
        }

        String query = "statistics_type:" + SolrLoggerServiceImpl.StatisticsType.VIEW.text();

        QueryResponse queryResponse = solrLoggerService.query(query, null, null, 1, 1, null,
                                                              null, null, null, "time", true, -1);

        SolrDocumentList results = queryResponse.getResults();
        if (results.isEmpty()) {
            return null;
        }

        defaultStartDate = formatDate(LocalDate.ofInstant(((Date) results.get(0).get("time")).toInstant(), UTC));
        return defaultStartDate;

    }

    public void setDefaultStartDate(String date) {
        LocalDate parsedDate = MultiFormatDateParser.parse(date).toLocalDate();
        if (parsedDate == null) {
            throw new IllegalStateException("Unsupported date " + date + " provided to TotalVisitPerPeriodGenerator");
        }
        this.defaultStartDate = formatDate(parsedDate);
    }

    private void validateStartAndEndDates(String startDate, String endDate) {
        if (!isBlankDate(startDate) && isNotValidDate(startDate)) {
            throw new IllegalArgumentException("The start date is not valid " + startDate + ", expected yyyy-MM-dd");
        }

        if (!isBlankDate(startDate) && isNotValidDate(endDate)) {
            throw new IllegalArgumentException("The end date is not valid " + endDate + ", expected yyyy-MM-dd");
        }
    }

    private boolean isNotValidDate(String date) {
        try {
            DATE_FORMAT.parse(date);
            return false;
        } catch (ParseException e) {
            return true;
        }
    }

    private boolean isBlankDate(String date) {
        return isBlank(date) || "null".equalsIgnoreCase(date);
    }

    private boolean isAfterToday(String date) {
        return MultiFormatDateParser.parse(date).toInstant().isAfter(Instant.now());
    }

    private String formatDate(LocalDate date) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
    }

    private boolean isNotSiteObject(DSpaceObject dso) {
        return dso.getType() != Constants.SITE;
    }

    @Override
    public String getReportType() {
        return UsageReportUtils.TOTAL_VISITS_PER_MONTH_REPORT_ID;
    }

    private Integer getDsoType(DSpaceObject dso) {
        return dsoType != null ? dsoType : dso.getType();
    }

    public void setDsoType(Integer dsoType) {
        this.dsoType = dsoType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public void setIncrement(int increment) {
        this.increment = increment;
    }

    public void setSolrLoggerService(SolrLoggerService solrLoggerService) {
        this.solrLoggerService = solrLoggerService;
    }

    public void setDiscoveryConfigurationService(DiscoveryConfigurationService discoveryConfigurationService) {
        this.discoveryConfigurationService = discoveryConfigurationService;
    }

    public void setMaxResults(int maxResults) {
        throw new IllegalStateException("Max results not supported by TotalVisitPerPeriodGenerator");
    }

}
