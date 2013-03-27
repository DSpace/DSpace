/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.statistics.content.DatasetDSpaceObjectGenerator;
import org.dspace.statistics.content.StatisticsDataVisits;
import org.dspace.workflow.DryadWorkflowUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.text.ParseException;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.StatisticsListing;
/**
 * Transformer that displays the recently submitted items on the dspace home page
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class MostViewedItem extends AbstractFiltersTransformer {

    private static final Logger log = Logger.getLogger(SiteRecentSubmissions.class);

    private static final Message T_head_recent_submissions =
            message("xmlui.ArtifactBrowser.SiteViewer.head_recent_submissions");
    private static final String T_head_view_count = "xmlui.statistics.view.count";
    private static final String T_head_view_title = "xmlui.statistics.view.title";
    private static final String T_head_viewed_item = "xmlui.statistics.most.viewed.items";
    /**
     * Display a single community (and refrence any sub communites or
     * collections)
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {

        //Try to find our dspace object
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        try
        {
            if(dso != null)
            {
                renderViewer(body, dso);
            }
            else
            {
                renderHome(body);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getView()
    {
        return "site";
    }
    String solr=  ConfigurationManager.getBooleanProperty("solr.log.server")+"/select/?q=type%3A2&facet=on&rows=0&facet.limit=-1&facet.field=id" ;

    /**
     * facet.limit=11&wt=javabin&rows=5&sort=dateaccessioned+asc&facet=true&facet.mincount=1&q=search.resourcetype:2&version=1
     *
     * @param object
     */
    @Override
    public void performSearch(DSpaceObject object) throws SearchServiceException, UIException, SQLException {

        if(queryResults != null)
        {
            return; // queryResults;
        }

        queryArgs = prepareDefaultFilters(getView());

        queryArgs.setQuery("search.resourcetype:" + Constants.ITEM);

        queryArgs.setRows(1000);

        String sortField = SearchUtils.getConfig().getString("recent.submissions.sort-option");
        if(sortField != null){
            queryArgs.setSortField(
                    sortField,
                    SolrQuery.ORDER.desc
            );
        }

        SearchService service = getSearchService();

        Context context = ContextUtil.obtainContext(objectModel);
        queryResults = service.search(context, queryArgs);

    }

    private boolean isAtLeastOneDataFileVisible(Context context, Item item) throws SQLException {
        Item[] datafiles = DryadWorkflowUtils.getDataFiles(context, item);
        for (Item i : datafiles) {
            String lift = ConfigurationManager.getProperty("embargo.field.lift");
            DCValue[] values = i.getMetadata(lift);
            if (values == null || values.length == 0)
                return true;

        }
        return false;
    }
    private void addDisplayListing(Division mainDiv, StatisticsListing display)
            throws SAXException, WingException, SQLException,
            SolrServerException, IOException, ParseException {

        String title = display.getTitle();

        Dataset dataset = display.getDataset();

        if (dataset == null) {
            /** activate dataset query */
            dataset = display.getDataset(context);
        }

        if (dataset != null)
        {
            String[][] matrix = dataset.getMatrixFormatted();

            java.util.List<String[]> values = retrieveResultList(title, dataset, matrix[0]);
            if (title != null)
            {
                mainDiv.setHead(message(title));
            }
            Division items = mainDiv.addDivision("items");
            Division count = mainDiv.addDivision("count");

            ReferenceSet referenceSet = items.addReferenceSet(
                    "most-viewed-items", ReferenceSet.TYPE_SUMMARY_LIST,
                    null, "most-viewed");
            org.dspace.app.xmlui.wing.element.List list = count.addList(
                    "most-viewed-count",
                    org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE, "most-viewed-count");

            items.setHead(message(T_head_view_title));

            count.setHead(message(T_head_view_count));

            for (String[] temp : values){
                 DSpaceObject dso = HandleManager.resolveToObject(context,temp[0]);
                 referenceSet.addReference(dso);
                 list.addItem().addContent(temp[1]);

            }
            if(!this.sitemapURI.contains("most_viewed_items")){
                mainDiv.addList("link-to-button").addItemXref("/most_viewed_items","View More");
            }

        }
    }
    public void renderHome(Body body) throws WingException {

        Division home = body.addDivision("home", "primary repository");
        Division division = home.addDivision("stats", "secondary stats");
        // division.setHead(T_head_title);

        try {
            /** List of the top 10 items for the entire repository **/
            StatisticsListing statListing = new StatisticsListing(
                    new StatisticsDataVisits());

            statListing.setTitle(T_head_viewed_item);
            statListing.setId("list1");

            //Adding a new generator for our top 1000 viewed items without a name length delimiter
            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
            if(this.sitemapURI.contains("most_viewed_items"))
            {
                dsoAxis.addDsoChild(Constants.ITEM, 1000, false, -1);
            }
            else
            {
                dsoAxis.addDsoChild(Constants.ITEM, 5, false, -1);
            }
            statListing.addDatasetGenerator(dsoAxis);

            //Render the list as a table
            addDisplayListing(division, statListing);

        } catch (Exception e) {
            log.error("Error occurred while creating statistics for home page", e);
        }

    }
    public void renderViewer(Body body, DSpaceObject dso) throws WingException {


    }
    private java.util.List<String[]> retrieveResultList(String title, Dataset dataset, String[] strings) throws SQLException {

        java.util.List<String[]> values = new ArrayList<String[]>();
        java.util.List<Map<String, String>> urls = dataset.getColLabelsAttrs();
        int j=0;
        int i=0;
        for (Map<String, String> map : urls)
        {
            for (Map.Entry<String, String> entry : map.entrySet())
            {
               if(i>10)
               {
                   break;
               }
                String url= entry.getValue();
                String suffix = url.substring(url.lastIndexOf("/handle/"));
                suffix=suffix.replace("/handle/","");
                String partOfURL = url.substring(0,url.lastIndexOf('/'));
                String prefix = ConfigurationManager.getProperty("dspace.url");


                DSpaceObject dso = HandleManager.resolveToObject(context, suffix);
                if(dso instanceof Item)
                {
                    if(dso!=null){

                        DCValue[] vals = ((Item)dso).getMetadata("dc", "title", null, Item.ANY);

                        if(vals != null && 0 < vals.length)
                        {
                            String[] temp = new String[3];
                            temp[0] = suffix;
                            temp[1]= strings[j];
                            values.add(temp);
                            i++;
                        }
                    }
                    j++;

                }


            }
        }
        return values;
    }
}