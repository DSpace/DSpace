/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static java.lang.Math.toIntExact;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.StatisticsSupportRest;
import org.dspace.app.rest.model.WorkflowStepStatisticsRest;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.statistics.WorkflowStepStatistics;
import org.dspace.statistics.service.WorkflowStatisticsService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Rest repository to retrieve WorkflowStep statistics.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component(StatisticsSupportRest.CATEGORY + "." + WorkflowStepStatisticsRest.NAME_PLURAL)
public class WorkflowStepStatisticsRestRepository extends DSpaceRestRepository<WorkflowStepStatisticsRest, String> {

    public static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private WorkflowStatisticsService workflowStatisticsService;

    @Autowired
    private CollectionService collectionService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public WorkflowStepStatisticsRest findOne(Context context, String id) {
        return workflowStatisticsService.findStepStatistics(context, id)
            .map(this::convertToRest)
            .orElse(null);
    }

    @SearchRestMethod(name = "byDateRange")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<WorkflowStepStatisticsRest> findByDateRange(Pageable pageable,
        @Parameter(value = "startDate") String startDate,
        @Parameter(value = "endDate") String endDate,
        @Parameter(value = "collection") String collectionId) {

        Context context = obtainContext();

        Collection collection = findCollection(collectionId);
        if (isNotBlank(collectionId) && collection == null) {
            throw new IllegalArgumentException("No collection found by id equals to " + collectionId);
        }

        List<WorkflowStepStatistics> statistics = workflowStatisticsService.findByDateRange(context,
            parseDate(startDate), parseDate(endDate), collection, toIntExact(pageable.getPageSize()));

        return converter.toRestPage(statistics, pageable, statistics.size(), utils.obtainProjection());
    }

    @SearchRestMethod(name = "current")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<WorkflowStepStatisticsRest> findCurrentWorkflows(Pageable pageable) {

        Context context = obtainContext();

        List<WorkflowStepStatistics> statistics = workflowStatisticsService.
            findCurrentWorkflows(context, toIntExact(pageable.getPageSize()));

        return converter.toRestPage(statistics, pageable, statistics.size(), utils.obtainProjection());
    }

    @Override
    public Page<WorkflowStepStatisticsRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "findAll");
    }

    private Collection findCollection(String collectionId) {
        try {
            return collectionService.find(obtainContext(), UUIDUtils.fromString(collectionId));
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private Date parseDate(String date) {
        try {
            return isNotBlank(date) ? DATE_FORMATTER.parse(date) : null;
        } catch (ParseException e) {
            throw new UnprocessableEntityException("The provided date has not a valid format. Expected: yyyy-MM-dd", e);
        }
    }

    private WorkflowStepStatisticsRest convertToRest(WorkflowStepStatistics workflowStepStatistics) {
        return converter.toRest(workflowStepStatistics, utils.obtainProjection());
    }

    @Override
    public Class<WorkflowStepStatisticsRest> getDomainClass() {
        return WorkflowStepStatisticsRest.class;
    }

}
