/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import org.dspace.content.Item;

/**
 * This DTO class is used to pass around the number of suggestions available from a specific source for a target
 * repository item
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SuggestionTarget {

    /** the item targeted */
    private Item target;

    /** source name of the suggestion */
    private String source;

    /** total count of suggestions for same target and source */
    private int total;

    public SuggestionTarget() {
    }

    /**
     * Wrap a target repository item (usually a person item) into a suggestion target.
     * 
     * @param item must be not null
     */
    public SuggestionTarget(Item item) {
        super();
        this.target = item;
    }

    /**
     * The suggestion target uses the concatenation of the source and target uuid separated by colon as id
     * 
     * @return the source:uuid of the wrapped item
     */
    public String getID() {
        return source + ":" + (target != null ? target.getID() : "");
    }

    public Item getTarget() {
        return target;
    }

    public void setTarget(Item target) {
        this.target = target;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

}