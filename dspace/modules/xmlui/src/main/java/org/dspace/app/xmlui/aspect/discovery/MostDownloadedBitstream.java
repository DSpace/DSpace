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
import org.dspace.app.xmlui.wing.element.File;
import org.dspace.app.xmlui.wing.element.List;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.text.ParseException;

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
public class MostDownloadedBitstream extends AbstractFiltersTransformer {


    private static final Logger log = Logger.getLogger(SiteRecentSubmissions.class);

    private static final String T_head_download_count = "xmlui.statistics.download.count";
    private static final String T_head_download_title = "xmlui.statistics.download.title";
    private static String myDataPkgColl = ConfigurationManager.getProperty("stats.datapkgs.coll");
    /**
     * Display a single community (and refrence any sub communites or
     * collections)
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {

        try {
            performSearch(null);
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }

        boolean includeRestrictedItems = ConfigurationManager.getBooleanProperty("harvest.includerestricted.rss", false);
        int numberOfItemsToShow= SearchUtils.getConfig().getInt("solr.recent-submissions.size", 5);

        Division home = body.addDivision("home", "primary repository");
        Division mostPopular =  home.addDivision("stats", "secondary stats");

        Division items = mostPopular.addDivision("items");
        Division count = mostPopular.addDivision("count");
        ReferenceSet referenceSet = items.addReferenceSet(
                "most-viewed-items", ReferenceSet.TYPE_SUMMARY_LIST,
                null, "most-viewed");
        org.dspace.app.xmlui.wing.element.List list = count.addList(
                "most-viewed-count",
                org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE, "most-viewed-count");

        items.setHead(message(T_head_download_title));
        count.setHead(message(T_head_download_count));

        String cacheFilePath = ConfigurationManager.getProperty("cached.dir") + "downloads.txt";
        long cacheTimeLimit = ConfigurationManager.getLongProperty("cached.timeout");

        int numberOfItemsAdded=0;
        String searchUrl="discover?sort_by=popularity&order=DESC&submit=Go";
        mostPopular.addList("most_popular").addItemXref(searchUrl,"View more");
        java.io.File cacheFile = new java.io.File(cacheFilePath);
        // the time before which we should refresh the cache:
        Date cacheRefreshTime = new Date(System.currentTimeMillis()-cacheTimeLimit);

        if (cacheFile.exists() && cacheRefreshTime.before(new Date(cacheFile.lastModified()))) {
            log.debug("cached file " + cacheFile.getAbsolutePath() + " is still good: " + new Date(cacheFile.lastModified()).toString() + " is after " + cacheRefreshTime.toString());
            java.util.List<String> lines = Files.readAllLines(Paths.get(cacheFilePath), StandardCharsets.UTF_8);
            for (String line : lines) {
                String[] fields = line.split("\\t");
                Item item = Item.find(context, Integer.valueOf(fields[0]));
                referenceSet.addReference(item);
                list.addItem().addContent(fields[1]);
            }
        } else {
            if (cacheFile.exists()) {
                log.debug("cached file " + cacheFile.getAbsolutePath() + " is bad: " + new Date(cacheFile.lastModified()).toString() + " is before " + cacheRefreshTime.toString());
            }
            try {
                performSearch(null);
            } catch (SearchServiceException e) {
                log.error(e.getMessage(), e);
            }
            log.debug("writing new cached file " + cacheFile.getAbsolutePath());
            BufferedWriter bw = new BufferedWriter(new FileWriter(cacheFile));
            for (SolrDocument doc : queryResults.getResults()) {
                DSpaceObject obj = SearchUtils.findDSpaceObject(context, doc);
                if(obj != null)
                {
                    // filter out Items that are not world-readable
                    if (!includeRestrictedItems) {
                        if (DryadWorkflowUtils.isAtLeastOneDataFileVisible(context, (Item)obj)) {
                            String downloads = doc.getFieldValue(SearchUtils.getConfig().getString("total.download.sort-option")).toString();
                            bw.write(Integer.valueOf(obj.getID()).toString() + "\t" + downloads);
                            bw.newLine();
                            referenceSet.addReference(obj);
                            list.addItem().addContent(downloads);
                            numberOfItemsAdded++;
                            if(numberOfItemsAdded==numberOfItemsToShow)
                                break;
                        }
                    }
                }
            }
            bw.close();
        }



    }

    public String getView()
    {
        return "site";
    }

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

        queryArgs.setRows(10);

        String sortField = SearchUtils.getConfig().getString("total.download.sort-option");
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
}



