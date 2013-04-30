/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.dryadwidgets;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.matching.Matcher;
import org.apache.cocoon.sitemap.PatternException;

/**
 *
 * @author dan
 */
public class WidgetBannerMatcher extends WidgetBannerLookup implements Matcher{
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WidgetBannerMatcher.class);

    @Override
    public Map match(String pattern, Map objectModel, Parameters parameters) throws PatternException {
            String publisher = parameters.getParameter("publisher","");
            String packageDOI = null;
        try {
            packageDOI = lookup(pattern, publisher, objectModel);
        } catch (SQLException ex) {
            log.error("Error looking up article DOI:", ex);
        }
        Map returnMap = new HashMap();
        returnMap.put(pattern, packageDOI);
        return returnMap;
    }

}
