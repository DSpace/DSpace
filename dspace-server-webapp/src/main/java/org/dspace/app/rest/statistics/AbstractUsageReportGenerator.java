/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.statistics;

/**
 * This is an abstract class that adds common configurable options for the generator
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public abstract class AbstractUsageReportGenerator implements UsageReportGenerator {
    /** Main query of the reports whose selection is done by filter queries only. */
    protected static final String MATCH_ALL_QUERY = "*:*";

    private String viewMode = "table";
    private int maxResults = 100;
    private String relation;

    /**
     * Sets the suggested view mode to apply when rendering the generated report.
     *
     * @param viewMode the view mode (e.g. "table")
     */
    public void setViewMode(String viewMode) {
        this.viewMode = viewMode;
    }

    /**
     * Returns the suggested view mode to apply when rendering the generated report.
     *
     * @return the configured view mode
     */
    public String getViewMode() {
        return viewMode;
    }

    /**
     * Sets the maximum number of results (points) to include in the generated report.
     *
     * @param maxResults the maximum number of results
     */
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    /**
     * Returns the maximum number of results (points) to include in the generated report.
     *
     * @return the configured maximum number of results
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Returns the name of the discovery relation configuration used to resolve related objects for this report,
     * or {@code null} when the report applies directly to the DSO.
     *
     * @return the relation configuration name, or null
     */
    public String getRelation() {
        return relation;
    }

    /**
     * Sets the name of the discovery relation configuration used to resolve related objects for this report.
     *
     * @param relation the relation configuration name
     */
    public void setRelation(String relation) {
        this.relation = relation;
    }

}

