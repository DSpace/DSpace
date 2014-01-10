/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.SearchLoggerAction;
import org.dspace.app.xmlui.utils.UIException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Returns the query for the advanced search so our SearchLoggerAction can log this
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AdvancedSearchLoggerAction extends SearchLoggerAction {

    private static final Logger log = Logger.getLogger(SearchLoggerAction.class);

    @Override
    protected List<String> getQueries(Request request) {
        try {
            String advancedSearchQuery = AdvancedSearchUtils.buildQuery(AdvancedSearchUtils.getSearchFields(request));
            if(!StringUtils.isBlank(advancedSearchQuery))
            {
                return Arrays.asList(advancedSearchQuery);
            }
        } catch (UIException e) {
            log.error(e.getMessage(), e);
        }
        return new ArrayList<String>();
    }
}
