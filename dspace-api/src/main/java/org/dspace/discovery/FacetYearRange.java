/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;

/**
 * Utilty class that represents the year range for a date facet
 */
public class FacetYearRange {
    private static final Pattern PATTERN = Pattern.compile("\\[(.*? TO .*?)\\]");

    private DiscoverySearchFilterFacet facet;
    private String dateFacet;
    private int oldestYear = -1;
    private int newestYear = -1;

    public FacetYearRange(DiscoverySearchFilterFacet facet) {
        this.facet = facet;
    }

    public String getDateFacet() {
        return dateFacet;
    }

    public int getOldestYear() {
        return oldestYear;
    }

    public int getNewestYear() {
        return newestYear;
    }

    public boolean isValid() {
        return oldestYear != -1 && newestYear != -1;
    }

    public void calculateRange(Context context, List<String> filterQueries, IndexableObject scope,
                               SearchService searchService, DiscoverQuery parentQuery) throws SearchServiceException {
        dateFacet = facet.getIndexFieldName() + ".year";
        //Get a range query so we can create facet queries ranging from our first to our last date
        //Attempt to determine our oldest & newest year by checking for previously selected filters
        lookupPreviousRangeInFilterQueries(filterQueries);

        //Check if we have found a range, if not then retrieve our first & last year using Solr
        if (oldestYear == -1 && newestYear == -1) {
            calculateNewRangeBasedOnSearchIndex(context, filterQueries, scope, searchService, parentQuery);
        }
    }

    private void lookupPreviousRangeInFilterQueries(List<String> filterQueries) {
        for (String filterQuery : filterQueries) {
            if (filterQuery.startsWith(dateFacet + ":")) {
                //Check for a range
                Matcher matcher = PATTERN.matcher(filterQuery);
                boolean hasPattern = matcher.find();
                if (hasPattern) {
                    filterQuery = matcher.group(0);
                    //We have a range
                    //Resolve our range to a first & last year
                    int tempOldYear = Integer.parseInt(filterQuery.split(" TO ")[0].replace("[", "").trim());
                    int tempNewYear = Integer.parseInt(filterQuery.split(" TO ")[1].replace("]", "").trim());

                    //Check if we have a further filter (or a first one found)
                    if (tempNewYear < newestYear || oldestYear < tempOldYear || newestYear == -1) {
                        oldestYear = tempOldYear;
                        newestYear = tempNewYear;
                    }

                } else {
                    if (filterQuery.indexOf(" OR ") != -1) {
                        //Should always be the case
                        filterQuery = filterQuery.split(" OR ")[0];
                    }
                    //We should have a single date
                    oldestYear = Integer.parseInt(filterQuery.split(":")[1].trim());
                    newestYear = oldestYear;
                    //No need to look further
                    break;
                }
            }
        }
    }

    private void calculateNewRangeBasedOnSearchIndex(Context context, List<String> filterQueries,
                                                     IndexableObject scope, SearchService searchService,
                                                     DiscoverQuery parentQuery) throws SearchServiceException {
        DiscoverQuery yearRangeQuery = new DiscoverQuery();
        yearRangeQuery.setDiscoveryConfigurationName(parentQuery.getDiscoveryConfigurationName());
        yearRangeQuery.setMaxResults(1);
        //Set our query to anything that has this value
        yearRangeQuery.addFieldPresentQueries(dateFacet);
        //Set sorting so our last value will appear on top
        yearRangeQuery.setSortField(dateFacet + "_sort", DiscoverQuery.SORT_ORDER.asc);
        yearRangeQuery.addFilterQueries(filterQueries.toArray(new String[filterQueries.size()]));
        yearRangeQuery.addSearchField(dateFacet);
        DiscoverResult lastYearResult = searchService.search(context, scope, yearRangeQuery);

        if (0 < lastYearResult.getIndexableObjects().size()) {
            List<DiscoverResult.SearchDocument> searchDocuments = lastYearResult
                .getSearchDocument(lastYearResult.getIndexableObjects().get(0));
            if (0 < searchDocuments.size() && 0 < searchDocuments.get(0).getSearchFieldValues(dateFacet).size()) {
                oldestYear = Integer.parseInt(searchDocuments.get(0).getSearchFieldValues(dateFacet).get(0));
            }
        }
        //Now get the first year
        yearRangeQuery.setSortField(dateFacet + "_sort", DiscoverQuery.SORT_ORDER.desc);
        DiscoverResult firstYearResult = searchService.search(context, scope, yearRangeQuery);
        if (0 < firstYearResult.getIndexableObjects().size()) {
            List<DiscoverResult.SearchDocument> searchDocuments = firstYearResult
                .getSearchDocument(firstYearResult.getIndexableObjects().get(0));
            if (0 < searchDocuments.size() && 0 < searchDocuments.get(0).getSearchFieldValues(dateFacet).size()) {
                newestYear = Integer.parseInt(searchDocuments.get(0).getSearchFieldValues(dateFacet).get(0));
            }
        }
    }

    public int getYearGap() {
        int gap = 1;
        //Attempt to retrieve our gap using the algorithm below
        int yearDifference = newestYear - oldestYear;
        if (yearDifference != 0) {
            gap = round((double) yearDifference / facet.getFacetLimit(), 10);
        }
        return gap;
    }

    private int round(double number, int multiple) {

        int result = new Double(Math.ceil(number)).intValue();

        //If not already multiple of given number

        if (result % multiple != 0) {

            int division = (result / multiple) + 1;

            result = division * multiple;

        }

        return result;
    }
}
