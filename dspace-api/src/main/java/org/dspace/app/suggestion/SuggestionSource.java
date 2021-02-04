/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

/**
 * 
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SuggestionSource {

    private String name;

    private int total;

    public SuggestionSource() {
    }

    /**
     * Summarize the available suggestions from a source.
     * 
     * @param the name must be not null
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