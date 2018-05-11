/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.sql.SQLException;

import org.dspace.content.Item;
/**
 * Interface for a strategy of style selection
 *  
 * @author Andrea Bollini
 * @version $Revision$
 */
public interface StyleSelection
{
    /**
     * Define which display style use for the item.
     * 
     * @param item
     * @return the style name to use for display simple metadata of the item 
     * @throws SQLException
     */
    public String getStyleForItem(Item item) throws SQLException;

    /**
     * Get the configuration of the style passed as argument.
     * The configuration has the following syntax: <code>schema.element[.qualifier|.*][(display-option)]</code> 
     * 
     * @param style
     * @return An array of Strings each containing a metadata field and if given a display option.
     */
    public String[] getConfigurationForStyle(String style);
}
