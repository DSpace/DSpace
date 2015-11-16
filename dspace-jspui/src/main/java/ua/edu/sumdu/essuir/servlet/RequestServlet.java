package ua.edu.sumdu.essuir.servlet;


import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSQuery;
import org.dspace.search.QueryArgs;
import org.dspace.search.QueryResults;
import org.dspace.sort.SortOption;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;


public class RequestServlet extends org.dspace.app.webui.servlet.DSpaceServlet {
    /** log4j category */
    private static Logger log = Logger.getLogger(RequestServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Get the query
        String query = request.getParameter("query");
        int start = UIUtil.getIntParameter(request, "start");
        int sortBy = UIUtil.getIntParameter(request, "sort_by");
        String order = request.getParameter("order");
        int rpp = UIUtil.getIntParameter(request, "rpp");

        // can't start earlier than 0 in the results!
        if (start < 0) {
            start = 0;
        }

        int itemCount = 0;

        Item[] resultsItems;

        QueryResults qResults = null;
        QueryArgs qArgs = new QueryArgs();
        SortOption sortOption = null;

        if (request.getParameter("etal") != null)
            qArgs.setEtAl(UIUtil.getIntParameter(request, "etal"));

        try
        {
            if (sortBy > 0)
            {
                sortOption = SortOption.getSortOption(sortBy);
                qArgs.setSortOption(sortOption);
            }

            if (SortOption.ASCENDING.equalsIgnoreCase(order))
            {
                qArgs.setSortOrder(SortOption.ASCENDING);
            }
            else
            {
                qArgs.setSortOrder(SortOption.DESCENDING);
            }
        }
        catch (Exception e)
        {
        }

        if (rpp < 0) {
            qArgs.setPageSize(Integer.MAX_VALUE);
        } else if (rpp > 0) {
            qArgs.setPageSize(rpp);
        }
        
        // Get the location parameter, if any
        String creation = request.getParameter("creation");

        // if the "advanced" flag is set, build the query string from the
        // multiple query fields
        if (creation != null) {
            query = qArgs.buildQuery(request);
        }

        // Ensure the query is non-null
        if (query == null) {
            query = "";
        }

        // get the start of the query results page
        //        List resultObjects = null;
        qArgs.setQuery(query);
        qArgs.setStart(start);

        // Perform the search
        qResults = DSQuery.doQuery(context, qArgs);

        // now instantiate the results and put them in their buckets
        for (int i = 0; i < qResults.getHitTypes().size(); i++)
        {
            Integer myType = (Integer) qResults.getHitTypes().get(i);

            // add the handle to the appropriate lists
            if (myType.intValue() == Constants.ITEM) {
                itemCount++;
            }
        }

        // Make objects from the handles - make arrays, fill them out
        resultsItems = new Item[itemCount];

        itemCount = 0;

        for (int i = 0; i < qResults.getHitTypes().size(); i++)
        {
            Integer myId    = (Integer) qResults.getHitIds().get(i);
            String myHandle = (String) qResults.getHitHandles().get(i);
            Integer myType  = (Integer) qResults.getHitTypes().get(i);

            // add the handle to the appropriate lists
            switch (myType.intValue())
            {
            case Constants.ITEM:
                if (myId != null) {
                    resultsItems[itemCount] = Item.find(context, myId);
                } else {
                    resultsItems[itemCount] = (Item) HandleManager.resolveToObject(context, myHandle);
                }

                if (resultsItems[itemCount] == null) {
                    throw new SQLException("Query \"" + query + "\" returned unresolvable item");
                }
                itemCount++;
                break;
            }
        }

        // total number of items
        int pageTotal = qResults.getHitCount();

        // Pass the results to the display JSP
        request.setAttribute("items", resultsItems);

        request.setAttribute("pagetotal", new Integer(pageTotal));

        // And the original query string
        request.setAttribute("query", query);

        request.setAttribute("order",  qArgs.getSortOrder());
        request.setAttribute("sortedBy", sortOption);

        if (qResults.getHitCount() == 0) {
            request.setAttribute("no_results", "yes");
        }

        if (creation != null) {
            JSPManager.showJSP(request, response, "/dsrequest/createrequest.jsp");
        } else {
            JSPManager.showJSP(request, response, "/dsrequest/dsrequest.jsp");
        }
    }

}
