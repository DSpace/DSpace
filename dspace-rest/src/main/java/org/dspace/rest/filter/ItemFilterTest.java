/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.filter;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Item Filter Use Case Interface.
 * Items will be evaluated against a set of filters.
 * 
 * @author Terry Brady, Georgetown University
 * 
 */
public interface ItemFilterTest {
    public String getName();
    public String getTitle();
    public String getDescription();
    public String getCategory();
    public boolean testItem(Context context, Item i);
}
