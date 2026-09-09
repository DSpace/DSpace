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
    private String viewMode = "table";
    private int maxResults = 100;
    private String relation;

    public void setViewMode(String viewMode) {
        this.viewMode = viewMode;
    }

    public String getViewMode() {
        return viewMode;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

}

