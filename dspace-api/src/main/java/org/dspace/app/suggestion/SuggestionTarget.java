/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.util.Map;
import java.util.UUID;

import org.dspace.content.Item;

/**
 * 
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SuggestionTarget {

    private Item target;

    private Map<String, Integer> totals;

    public SuggestionTarget() {
    }

    /**
     * Wrap a target person into a suggestion target.
     * 
     * @param item must be not null
     */
    public SuggestionTarget(Item item) {
        super();
        this.target = item;
    }

    /**
     * The suggestion target uses the same uuid than the person item that wrap
     * 
     * @return the uuid of the wrapped item
     */
    public UUID getID() {
        return target.getID();
    }

    public Item getTarget() {
        return target;
    }

    public void setTarget(Item target) {
        this.target = target;
    }

    public Map<String, Integer> getTotals() {
        return totals;
    }

    public void setTotals(Map<String, Integer> totals) {
        this.totals = totals;
    }

}