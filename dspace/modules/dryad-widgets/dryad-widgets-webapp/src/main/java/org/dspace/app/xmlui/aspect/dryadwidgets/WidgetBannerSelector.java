/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.dryadwidgets;

import java.sql.SQLException;
import java.util.Map;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.selection.Selector;
import org.apache.log4j.Logger;

/**
 * This simple selector checks if a data package exists for a given article DOI.
 * It returns true if the identifier is found, false if not.
 * Can be used to determine which widget banner to render.
 *
 * The publisher parameter must be provided, but is not currently recorded here.
 * 
 * @author Dan Leehr
 */

public class WidgetBannerSelector extends WidgetBannerLookup implements Selector
{

    private static Logger log = Logger.getLogger(WidgetBannerSelector.class);

    /**
     * Determine if the provided identifier is resolvable
     */
    public boolean select(String expression, Map objectModel,
            Parameters parameters) {
            String referrer = parameters.getParameter("referrer","");
            String pubId = expression;
            String packageDOI = null;
        try {
            packageDOI = lookup(pubId, referrer, objectModel);
        } catch (SQLException ex) {
            log.error("Error looking up article identifier:", ex);
        }
        return packageDOI != null;
    }

}
