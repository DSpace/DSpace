/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

/**
 * This DTO class is used to pass around the number of items interested by suggestion provided by a specific source
 * (i.e. openaire)
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SuggestionSource {

    /** source name of the suggestion */
    private String name;

    /** number of targeted items */
    private int total;

    public SuggestionSource() {
    }

    /**
     * Summarize the available suggestions from a source.
     * 
     * @param name the name must be not null
     */
    public SuggestionSource(String name) {
        super();
        this.name = name;
    }

    public String getID() {
        return name;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

}