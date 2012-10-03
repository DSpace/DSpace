/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DatasetSearchGenerator extends DatasetTypeGenerator {

    public static enum Mode {
   		SEARCH_OVERVIEW ("search_overview"),
   		SEARCH_OVERVIEW_TOTAL ("search_overview_total");

        private final String text;

        Mode(String text) {
   	        this.text = text;
   	    }
   	    public String text()   { return text; }
    }

    private Mode mode;
    private boolean percentage = false;
    private boolean retrievePageViews;

    public boolean isRetrievePageViews() {
        return retrievePageViews;
    }

    public void setRetrievePageViews(boolean retrievePageViews) {
        this.retrievePageViews = retrievePageViews;
    }

    public void setPercentage(boolean percentage){
        this.percentage = percentage;
    }

    public boolean isPercentage() {
        return percentage;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }
}
