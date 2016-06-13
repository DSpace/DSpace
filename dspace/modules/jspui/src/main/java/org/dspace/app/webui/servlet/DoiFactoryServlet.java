package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.webui.util.DOIQueryConfigurator;
import org.dspace.app.webui.util.DoiFactoryUtils;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.event.Event;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.dspace.utils.DSpace;

public class DoiFactoryServlet extends DSpaceServlet
{
    /** log4j category */
    public static Logger log = Logger.getLogger(DoiFactoryServlet.class);

    DSpace dspace = new DSpace();

    SearchService searcher = dspace.getServiceManager().getServiceByName(
            SearchService.class.getName(), SearchService.class);

    IndexingService indexer = dspace.getServiceManager().getServiceByName(
            IndexingService.class.getName(), IndexingService.class);

    public static int DOI_ALL = 1;

    public static int DOI_ANY = 0;

    public static int EXCLUDE_ANY = 2;

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        doDSGet(context, request, response);
    }

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

        String pathString = request.getPathInfo();
        String[] pathInfo = pathString.split("/", 2);
        String criteria = pathInfo[1];

        // After loaded grid to display logic here tries discover if target
        // must be choice or not.

        int submit = UIUtil.getIntParameter(request, "submit");

        DOIQueryConfigurator doiConfigurator = new DOIQueryConfigurator();
        String query = doiConfigurator.getQuery(criteria, new String[] {});
        List<String> filters = doiConfigurator.getFilters(criteria);
        int sortBy = doiConfigurator.getSortBy(request, criteria);
        String order = doiConfigurator.getOrder(request, criteria);
        int rpp = doiConfigurator.getRPP(request, criteria);
        int etAl = doiConfigurator.getEtAl(request, criteria);
        String orderfield = sortBy != -1 ? "bi_sort_" + sortBy + "_sort" : null;
        boolean ascending = SortOption.ASCENDING.equalsIgnoreCase(order);
        EPerson currentUser = context.getCurrentUser();

        if (submit != -1)
        {
            // perform action

            if (submit == DOI_ALL)
            {
                SolrDocumentList docs = search(0, query, filters,
                        Integer.MAX_VALUE, orderfield, ascending);

                // Pass the results to the display JSP
                List<Item> results = DoiFactoryUtils.getItemsFromSolrResult(
                        docs, context);
                Map<Integer, String> customDoiMaps = new HashMap<Integer, String>();
                for (Item ii : results)
                {
                    int id = ii.getID();
                    fillDoiMap(request, customDoiMaps, id);
                }
                try
                {
                    buildDOIAndAddToQueue(context, request, response, results,
                            currentUser, criteria, null);
                }
                catch (SearchServiceException e)
                {
                    log.error(e.getMessage(), new RuntimeException(e));
                }

                response.sendRedirect(request.getContextPath()
                        + "/dspace-admin/doi");
            }
            else if (submit == DOI_ANY)
            {

                int[] items = UIUtil.getIntParameters(request, "builddoi");

                Map<Integer, String> customDoiMaps = new HashMap<Integer, String>();
                for (Integer id : items)
                {
                    fillDoiMap(request, customDoiMaps, id);
                }
                List<Item> results = DoiFactoryUtils.getItems(context, items);

                try
                {
                    buildDOIAndAddToQueue(context, request, response, results,
                            currentUser, criteria, customDoiMaps);
                }
                catch (SearchServiceException e)
                {
                    log.error(e.getMessage(), new RuntimeException(e));
                }

                response.sendRedirect(request.getContextPath()
                        + "/dspace-admin/doifactory/" + criteria);
            }
            else if (submit == EXCLUDE_ANY)
            {

                int[] items = UIUtil.getIntParameters(request, "builddoi");

                List<Item> results = DoiFactoryUtils.getItems(context, items);
                Map<Integer, String> customDoiMaps = new HashMap<Integer, String>();
                for (Integer id : items)
                {
                    fillDoiMap(request, customDoiMaps, id);
                }
                try
                {
                    excludeFromList(context, results, customDoiMaps);
                }
                catch (SearchServiceException e)
                {
                    log.error(e.getMessage(), new RuntimeException(e));
                }

                response.sendRedirect(request.getContextPath()
                        + "/dspace-admin/doifactory/" + criteria);

            }
        }
        else
        {

            int start = UIUtil.getIntParameter(request, "start") != -1 ? UIUtil
                    .getIntParameter(request, "start") : 0;

            SolrDocumentList docs = search(start, query, filters, rpp,
                    orderfield, ascending);

            // Pass the results to the display JSP
            List<Item> results = DoiFactoryUtils.getItemsFromSolrResult(docs,
                    context);

            // Pass in some page qualities
            // total number of pages
            int pageTotal = 0;

            if (docs != null)
            {
                pageTotal = (int) (1 + ((docs.getNumFound() - 1) / rpp));
            }
            // current page being displayed
            int pageCurrent = 1 + (start / rpp);

            // pageLast = min(pageCurrent+9,pageTotal)
            int pageLast = ((pageCurrent + 9) > pageTotal) ? pageTotal
                    : (pageCurrent + 9);

            // pageFirst = max(1,pageCurrent-9)
            int pageFirst = ((pageCurrent - 9) > 1) ? (pageCurrent - 9) : 1;

            SortOption sortOption = null;
            if (sortBy > 0)
            {
                try
                {
                    sortOption = SortOption.getSortOption(sortBy);
                }
                catch (SortException e)
                {
                    log.error(e.getMessage(), e);
                }
            }

            // Pass the result
            Item[] realresult = null;
            Map<Integer, String> doi2items = new HashMap<Integer, String>();

            if (results != null && !results.isEmpty())
            {
                realresult = results.toArray(new Item[results.size()]);
                for (Item real : realresult)
                {
                    doi2items.put(real.getID(),
                            DoiFactoryUtils.buildDoi(context, criteria, real)
                                    .trim());
                }
            }

            request.setAttribute("items", realresult);
            request.setAttribute("doi2items", doi2items);
            request.setAttribute("prefixDOI", DoiFactoryUtils.PREFIX_DOI);
            request.setAttribute("pagetotal", new Integer(pageTotal));
            request.setAttribute("pagecurrent", new Integer(pageCurrent));
            request.setAttribute("pagelast", new Integer(pageLast));
            request.setAttribute("pagefirst", new Integer(pageFirst));

            request.setAttribute("order", order);
            request.setAttribute("sortedBy", sortOption);
            request.setAttribute("start", start);
            request.setAttribute("rpp", rpp);
            request.setAttribute("etAl", etAl);
            request.setAttribute("total", docs.getNumFound());
            request.setAttribute("type", criteria);
            JSPManager.showJSP(request, response, "/doi/factoryDoiResults.jsp");
            return;
        }
    }

    private void fillDoiMap(HttpServletRequest request,
            Map<Integer, String> customDoiMaps, Integer ii)
    {
        String customdoi = request.getParameter("custombuilddoi_" + ii);
        if (customdoi != null && !customdoi.isEmpty())
        {
            customDoiMaps.put(ii, DoiFactoryUtils.PREFIX_DOI + customdoi);
        }
    }

    /**
     * Add same special metadata: - item.addMetadata("dc", "utils", "nodoi",
     * Item.ANY, "true"); - item.addMetadata("dc", "identifier", "doi",
     * Item.ANY, <doi>);
     * 
     * The second metadata will add only if passed by view.
     * 
     * @param context
     * @param results
     * @param dois
     * @throws SQLException
     * @throws AuthorizeException
     * @throws SearchServiceException
     */
    private void excludeFromList(Context context, List<Item> results,
            Map<Integer, String> dois) throws SQLException, AuthorizeException,
            SearchServiceException
    {
        for (Item item : results)
        {
            String doi = null;
            if (dois != null)
            {
                doi = dois.get(item.getID());
            }
            if (doi != null && !doi.isEmpty())
            {
                item.clearMetadata("dc", "identifier", "doi", Item.ANY);
                item.addMetadata("dc", "identifier", "doi", Item.ANY, doi);
            }
            item.addMetadata("dc", "utils", "nodoi", null, "true");
            item.update();
        }

        context.commit();
        indexer.commit();
    }

    private SolrDocumentList search(int start, String query,
            List<String> filters, int rpp, String orderfield, boolean ascending)
    {

        // can't start earlier than 0 in the results!
        if (start < 0)
        {
            start = 0;
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        DoiFactoryUtils.prepareDefaultSolrQuery(solrQuery);
        solrQuery.setFields("search.resourceid", "search.resourcetype",
                "handle");
        solrQuery.setStart(start);
        solrQuery.setRows(rpp);
        if (orderfield == null)
        {
            orderfield = "score";
        }
        solrQuery.addSortField(orderfield, ascending ? SolrQuery.ORDER.asc
                : SolrQuery.ORDER.desc);

        if (filters != null)
        {
            for (String filter : filters)
            {
                solrQuery.addFilterQuery(filter);
            }
        }

        QueryResponse rsp = null;
        SolrDocumentList publications = null;

        try
        {
            rsp = searcher.search(solrQuery);
            publications = rsp.getResults();
        }
        catch (SearchServiceException e)
        {
            log.error(e.getMessage(), e);
        }
        return publications;
    }

    protected void buildDOIAndAddToQueue(Context context,
            HttpServletRequest request, HttpServletResponse response,
            List<Item> items, EPerson eperson, String type,
            Map<Integer, String> mapDoi) throws IOException, ServletException,
            SQLException, AuthorizeException, SearchServiceException
    {

        DoiFactoryUtils.internalBuildDOIAndAddToQueue(context, items, eperson,
                type, mapDoi);

        // Creating Add event to force index
        for (Item item : items)
        {
            context.addEvent(new Event(Event.UPDATE_FORCE, Constants.ITEM, item
                    .getID(), item.getHandle()));
        }

        context.commit();
        indexer.commit();

    }

}
