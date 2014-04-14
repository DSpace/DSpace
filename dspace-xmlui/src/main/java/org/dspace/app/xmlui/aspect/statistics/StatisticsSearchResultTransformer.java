/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statistics;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.aspect.artifactbrowser.AdvancedSearchUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.handle.HandleManager;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Transformer that adds a hidden form
 * which will be submitted each time an dspace object link is clicked on a lucene search page
 * This form will ensure that the results clicked after each search are logged for the search statistics
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class StatisticsSearchResultTransformer extends AbstractDSpaceTransformer {


    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException, ProcessingException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        StringBuilder formUrl = new StringBuilder();
        formUrl.append(request.getContextPath());
        DSpaceObject scope = getScope();
        if(scope != null){
            formUrl.append("/handle/").append(scope.getHandle());
        }
        if(parameters.getParameterAsBoolean("advanced-search", false))
        {
            formUrl.append("/advanced-search");
        } else {
            formUrl.append("/search");
        }

        Division mainForm = body.addInteractiveDivision("dso-display", formUrl.toString(), Division.METHOD_POST, "");

        mainForm.addHidden("current-scope").setValue(scope == null ? "" : scope.getHandle());
        //Indicate that the form we are submitting lists search results
        mainForm.addHidden("search-result").setValue(Boolean.TRUE.toString());
        mainForm.addHidden("query").setValue(getQuery());
        if(!StringUtils.isBlank(request.getParameter("rpp"))){
            mainForm.addHidden("rpp").setValue(Integer.parseInt(request.getParameter("rpp")));
        }
        if(!StringUtils.isBlank(request.getParameter("sort_by"))){
            mainForm.addHidden("sort_by").setValue(request.getParameter("sort_by"));
        }
        if(!StringUtils.isBlank(request.getParameter("order"))){
            mainForm.addHidden("order").setValue(request.getParameter("order"));
        }
        if(!StringUtils.isBlank(request.getParameter("page"))){
            mainForm.addHidden("page").setValue(Integer.parseInt(request.getParameter("page")));
        }
    }

    private String getQuery() throws UIException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        if(parameters.getParameterAsBoolean("advanced-search", false)){
            return AdvancedSearchUtils.buildQuery(AdvancedSearchUtils.getSearchFields(request));
        }else{
            String query = decodeFromURL(request.getParameter("query"));
            if (query == null)
            {
                return "";
            }
            return query;
        }
    }

    /**
     * Determine the current scope. This may be derived from the current url
     * handle if present or the scope parameter is given. If no scope is
     * specified then null is returned.
     *
     * @return The current scope.
     */
    protected DSpaceObject getScope() throws SQLException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String scopeString = request.getParameter("scope");

        // Are we in a community or collection?
        DSpaceObject dso;
        if (scopeString == null || "".equals(scopeString))
        {
            // get the search scope from the url handle
            dso = HandleUtil.obtainHandle(objectModel);
        }
        else
        {
            // Get the search scope from the location parameter
            dso = HandleManager.resolveToObject(context, scopeString);
        }

        return dso;
    }
}
