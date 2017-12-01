/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.viewArtifacts;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.matching.Matcher;
import org.apache.cocoon.sitemap.PatternException;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Matcher used to determine which transformers are used to render the home page for a community/collection
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class ContainerHomePageMatcher implements Matcher {

    private static final Logger log = Logger.getLogger(ContainerHomePageMatcher.class);

    @Override
    public Map match(String pattern, Map objectModel, Parameters parameters) throws PatternException {
        boolean not = false;
        int action = -1; // the action to check

        if (pattern.startsWith("!"))
        {
            not = true;
            pattern = pattern.substring(1);
        }

        if(pattern.equals("discoveryRecentSubmissions") || pattern.equals("metadata"))
        {
            try {
                boolean isHomePageActive;
                DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(HandleUtil.obtainHandle(objectModel));
                if(discoveryConfiguration.getRecentSubmissionConfiguration() != null && discoveryConfiguration.getRecentSubmissionConfiguration().getUseAsHomePage())
                {
                    isHomePageActive = !pattern.equals("metadata");
                } else{
                    isHomePageActive = pattern.equals("metadata");
                }
                if(isHomePageActive ^ not)
                {
                    return new HashMap();
                }else{
                    return null;
                }
            } catch (SQLException e) {
                log.error("SQL exception while determining home page", e);

            }
        }
        throw new IllegalArgumentException();
    }
}
