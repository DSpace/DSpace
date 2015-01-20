/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.log4j.Logger;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.element.Body;

import java.sql.SQLException;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.authorize.AuthorizeException;

import org.xml.sax.SAXException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.aspect.discovery.AbstractFiltersTransformer;
import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;
import org.dspace.app.xmlui.utils.ContextUtil;
import static org.dspace.app.xmlui.wing.AbstractWingTransformer.*;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.workflow.DryadWorkflowUtils;

/**
 *
 * @author Nathan Day
 */
public class TopTenDownloads extends AbstractFiltersTransformer {
    
    private static final Logger log = Logger.getLogger(TopTenDownloads.class);
    private static final Message T_head = message("xmlui.JournalLandingPage.TopTenRecentDeposits.panel_head"); 
    
    ArrayList<DSpaceObject> references = new ArrayList<DSpaceObject>();
    ArrayList<String> downloads = new ArrayList<String>();
        
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        // ------------------
        // Top 10 downloads
        // 
        // ------------------
        Division div = body.addDivision(MOST_TOPTEN_DEPOSITS_DIV);
        div.setHead(T_head);
        ReferenceSet refs = div.addReferenceSet(MOST_TOPTEN_DEPOSITS_REFS, null);
        List list = div.addList("most-viewed-count", List.TYPE_SIMPLE, "most-viewed-count");

        try {
            performSearch(null);
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
            return;
        }
        for (DSpaceObject ref : references)
            refs.addReference(ref);
        for (String s : downloads)
            list.addItem(s);
    }

    /**
     * 
     *
     * @param object
     */
    @Override
    public void performSearch(DSpaceObject object) throws SearchServiceException, UIException {
        if (queryResults != null) return;
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
        Context c = null;
        try {
            c = ContextUtil.obtainContext(objectModel);
        } catch (SQLException ex) {
            log.error(ex.getMessage());
        }
        queryResults = (QueryResponse) service.search(c, queryArgs);
        boolean includeRestrictedItems = ConfigurationManager.getBooleanProperty("harvest.includerestricted.rss", false);
        int numberOfItemsToShow= SearchUtils.getConfig().getInt("solr.recent-submissions.size", 5);
        ExtendedProperties config = SearchUtils.getConfig();
        if (queryResults != null && !includeRestrictedItems)  {
            for (Iterator<SolrDocument> it = queryResults.getResults().iterator(); it.hasNext() && references.size() < numberOfItemsToShow;) {
                SolrDocument doc = it.next();
                DSpaceObject obj = null;
                try {
                    obj = SearchUtils.findDSpaceObject(context, doc);
                } catch (SQLException ex) {
                    log.error(ex.getMessage());
                }
                try {
                    if (obj != null
                        && DryadWorkflowUtils.isAtLeastOneDataFileVisible(context, (Item) obj))
                    {
                        references.add(obj);
                        downloads.add(doc.getFieldValue(config.getString("total.download.sort-option")).toString());
                    }
                } catch (SQLException ex) {
                    log.error(ex.getMessage());
                }
            }
        }
    }

    @Override
    public String getView() {
        return "site";
    }
}
