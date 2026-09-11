/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import static java.lang.String.valueOf;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.solr.common.params.FacetParams.FACET_LIMIT;
import static org.dspace.xmlworkflow.service.XmlWorkflowService.ITEM_STEP;
import static org.dspace.xmlworkflow.service.XmlWorkflowService.SUBMIT_STEP;
import static org.dspace.xmlworkflow.service.XmlWorkflowService.WORKSPACE_STEP;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResult.FacetPivotResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.statistics.SolrLoggerServiceImpl.StatisticsType;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;
import org.dspace.statistics.service.SolrLoggerService;
import org.dspace.statistics.service.WorkflowStatisticsService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link WorkflowStatisticsService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class WorkflowStatisticsServiceImpl implements WorkflowStatisticsService {

    private static final String ITEM_OR_WORKSPACE_STATISTICS_FACET_PIVOT = "workflowStep,workflowAction";

    private static final String STEP_STATISTICS_FACET_PIVOT = "previousWorkflowStep,previousWorkflowAction";

    private static final String OWNER_STATISTICS_FACET_PIVOT = "actor,previousWorkflowStep,previousWorkflowAction";

    private static final String CURRENT_STEPS_STATISTICS_FACET_PIVOT = "step_keyword,action_keyword";

    @Autowired
    private SolrLoggerService solrLoggerService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public Optional<WorkflowStepStatistics> findStepStatistics(Context context, String stepName) {

        boolean isItemOrWorkspaceStep = ITEM_STEP.equals(stepName) || WORKSPACE_STEP.equals(stepName);

        String queryFilter = null;
        String facetPivot = null;

        if (isItemOrWorkspaceStep) {
            queryFilter = composeQueryFilter() + " AND workflowStep: " + stepName;
            facetPivot = ITEM_OR_WORKSPACE_STATISTICS_FACET_PIVOT;
        } else {
            queryFilter = composeQueryFilter() + " AND previousWorkflowStep: " + stepName;
            facetPivot = STEP_STATISTICS_FACET_PIVOT;
        }

        FacetPivotResult[] facetPivotResults = queryWithPivotField(queryFilter, 1, facetPivot);
        if (facetPivotResults.length == 0) {
            return Optional.empty();
        }

        return Optional.of(convertToStepStatistics(facetPivotResults[0]));
    }

    @Override
    public Optional<WorkflowOwnerStatistics> findOwnerStatistics(Context context, UUID ownerId) {

        String queryFilter = composeQueryFilter() + " AND actor:" + ownerId;
        FacetPivotResult[] facetPivotResults = queryWithPivotField(queryFilter, 1, OWNER_STATISTICS_FACET_PIVOT);
        if (facetPivotResults.length == 0) {
            return Optional.empty();
        }

        return convertToOwnerStatistics(context, facetPivotResults[0]);

    }

    @Override
    public List<WorkflowStepStatistics> findCurrentWorkflows(Context context, int size) {
        return searchClaimedAndPoolTasks(context, size).stream()
            .map(this::convertToStepStatistics)
            .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowStepStatistics> findByDateRange(Context context, Date startDate, Date endDate,
        Collection collection, int limit) {

        String queryFilter = composeQueryFilter(startDate, endDate, collection);

        FacetPivotResult[] stepPivots = queryWithPivotField(queryFilter, limit, STEP_STATISTICS_FACET_PIVOT);

        String itemOrWorkspaceFilter = queryFilter + " AND workflowStep: (" + ITEM_STEP + " OR " + WORKSPACE_STEP + ")";

        FacetPivotResult[] itemOrWorkspaceStepPivots = queryWithPivotField(itemOrWorkspaceFilter, limit,
            ITEM_OR_WORKSPACE_STATISTICS_FACET_PIVOT);

        return Stream.concat(Stream.of(stepPivots), Stream.of(itemOrWorkspaceStepPivots))
            .map(this::convertToStepStatistics)
            .filter(stepStatistics -> stepStatistics.getCount() > 0L)
            .sorted(Comparator.comparing(WorkflowStepStatistics::getCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowOwnerStatistics> findOwnersByDateRange(Context context, Date startDate, Date endDate,
        Collection collection, int limit) {

        String queryFilter = composeQueryFilter(startDate, endDate, collection);
        queryFilter = queryFilter + " AND -previousWorkflowStep:" + SUBMIT_STEP;

        FacetPivotResult[] facetPivotFields = queryWithPivotField(queryFilter, limit, OWNER_STATISTICS_FACET_PIVOT);

        return Arrays.stream(facetPivotFields)
            .flatMap(pivotObjectCount -> convertToOwnerStatistics(context, pivotObjectCount).stream())
            .collect(Collectors.toList());
    }

    private List<FacetPivotResult> searchClaimedAndPoolTasks(Context context, int size) {

        DiscoverQuery query = new DiscoverQuery();
        query.addDSpaceObjectFilter(IndexableClaimedTask.TYPE);
        query.addDSpaceObjectFilter(IndexablePoolTask.TYPE);
        query.addFacetPivot(CURRENT_STEPS_STATISTICS_FACET_PIVOT);
        query.addProperty("f." + CURRENT_STEPS_STATISTICS_FACET_PIVOT.split(",")[0] + "." + FACET_LIMIT, valueOf(size));

        try {
            DiscoverResult discoverResult = searchService.search(context, query);
            return discoverResult.getFacetPivotResult(CURRENT_STEPS_STATISTICS_FACET_PIVOT);
        } catch (SearchServiceException e) {
            throw new RuntimeException(e);
        }

    }

    private String composeQueryFilter(Date startDate, Date endDate, Collection collection) {
        return Stream.of(of(composeQueryFilter()), getDateFilter(startDate, endDate), getCollectionFilter(collection))
            .flatMap(Optional::stream)
            .collect(Collectors.joining(" AND "));
    }


    private String composeQueryFilter() {
        String baseFilter = getWorkflowTypeFilter() + " AND previousActionRequiresUI: true";
        return getActionFilter()
            .map(actionFilter -> baseFilter + " AND " + actionFilter)
            .orElse(baseFilter);
    }

    private Optional<String> getCollectionFilter(Collection collection) {
        return Optional.ofNullable(collection)
            .map(col -> "owningColl:" + collection.getID());
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

    private FacetPivotResult[] queryWithPivotField(String filter, int limit, String pivotField) {
        try {
            return solrLoggerService.queryFacetPivotField("*:*", filter, pivotField, limit, false, null, 0);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<WorkflowOwnerStatistics> convertToOwnerStatistics(Context context, FacetPivotResult pivot) {

        String owner = pivot.getValue();
        long count = pivot.getCount();

        return findUserById(context, UUIDUtils.fromString(owner))
            .map(user -> new WorkflowOwnerStatistics(user, count, createActionCountMapFromActorPivot(pivot)));

    }

    private WorkflowStepStatistics convertToStepStatistics(FacetPivotResult pivot) {
        return new WorkflowStepStatistics(pivot.getValue(), pivot.getCount(), createActionCountMapFromStepPivot(pivot));
    }

    private Map<String, Long> createActionCountMapFromStepPivot(FacetPivotResult facetPivotResult) {
        return Arrays.stream(facetPivotResult.getPivot())
            .collect(Collectors.toMap(FacetPivotResult::getValue, FacetPivotResult::getCount));
    }

    private Map<String, Long> createActionCountMapFromActorPivot(FacetPivotResult pivotResult) {
        Map<String, Long> actionCounts = new HashMap<String, Long>();
        for (FacetPivotResult stepFacetPivot : pivotResult.getPivot()) {
            for (FacetPivotResult actionFacetPivot : stepFacetPivot.getPivot()) {
                String actionName = stepFacetPivot.getValue() + "." + actionFacetPivot.getValue();
                actionCounts.put(actionName, actionFacetPivot.getCount());
            }
        }
        return actionCounts;
    }

    private String getWorkflowTypeFilter() {
        return "statistics_type:" + StatisticsType.WORKFLOW.text();
    }

    private Optional<String> getActionFilter() {
        String[] actionsToFilter = configurationService.getArrayProperty("statistics.workflow.actions-to-filter");
        if (ArrayUtils.isEmpty(actionsToFilter)) {
            return Optional.empty();
        }
        return Optional.of("-previousWorkflowAction: ( " + String.join(" OR ", actionsToFilter) + " )");
    }

    private Optional<EPerson> findUserById(Context context, UUID uuid) {
        try {
            return ofNullable(ePersonService.find(context, uuid));
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

}
