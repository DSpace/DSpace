/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.dspace.content.Item;
import org.dspace.core.Context;
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
    public String getStyleForItem(Context context, Item item, HttpServletRequest request) throws SQLException;

    /**
     * Get the configuration of the style passed as argument.
     * The configuration has the following syntax: <code>schema.element[.qualifier|.*][(display-option)]</code> 
     * 
     * @param style
     */
    public String getConfigurationForStyle(Context context, String style, HttpServletRequest request) throws SQLException;
    
    /**
     * Return true if the requested configuration is defined, it doesn't fallback to default configuration
     * @param style
     * @return
     */
    public boolean isConfigurationDefinedForStyle(Context context, String style, HttpServletRequest request) throws SQLException;
}
