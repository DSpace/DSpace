/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import java.io.IOException;
import java.io.Serializable;
import org.apache.log4j.Logger;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.commons.collections.ExtendedProperties;
import org.xml.sax.SAXException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.aspect.discovery.AbstractFiltersTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import static org.dspace.app.xmlui.wing.AbstractWingTransformer.message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.workflow.DryadWorkflowUtils;

/**
 *
 * @author Nathan Day
 */
public class MostRecentDeposits extends AbstractFiltersTransformer {
    
    private static final Logger log = Logger.getLogger(MostRecentDeposits.class);
    private final static SimpleDateFormat fmt = new SimpleDateFormat(fmtDateView);
    
    private static final Message T_mostRecent = message("xmlui.JournalLandingPage.MostRecentDeposits.panel_head");
    
    ArrayList<DSpaceObject> references;
    ArrayList<String> dates;
    
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        // ------------------
        // Most recent deposits
        // 
        // ------------------
        Division mostRecent = body.addDivision(MOST_RECENT_DEPOSITS_DIV);
        mostRecent.setHead(T_mostRecent);
        
        Division items = mostRecent.addDivision("items");
        Division date = mostRecent.addDivision("date");
        
        ReferenceSet refs = items.addReferenceSet(MOST_RECENT_DEPOSITS_REFS, "summaryList");
        references = new ArrayList<DSpaceObject>();
        dates = new ArrayList<String>();
        try {
            performSearch(null);
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }
        if (references.size() == 0) {
            vals.addItem(EMPTY_VAL);
        } else {
            for (DSpaceObject ref : references)
                refs.addReference(ref);
            for (String s : dates)
                vals.addItem(s);
        }
        references = null;
        dates = null;
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
        queryArgs.setQuery("search.resourcetype:" + Constants.ITEM + " AND prism.publicationName:" + journalName);
        queryArgs.setRows(10);
        String sortField = SearchUtils.getConfig().getString("recent.submissions.sort-option");
        if(sortField != null){
            queryArgs.setSortField(
                    sortField,
                    SolrQuery.ORDER.desc
            );
        }
        SearchService service = (SearchService) getSearchService();
        try {
            queryResults = service.search(context, queryArgs);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        
        
        boolean includeRestrictedItems = ConfigurationManager.getBooleanProperty("harvest.includerestricted.rss", false);
        int numberOfItemsToShow= SearchUtils.getConfig().getInt("solr.recent-submissions.size", 5);
        ExtendedProperties config = SearchUtils.getConfig();
        Object o;
        if (queryResults != null && !includeRestrictedItems)  {
            for (SolrDocument doc : queryResults.getResults()) {
                if (references.size() > numberOfItemsToShow) break;
                DSpaceObject obj = null;
                try {
                    obj = SearchUtils.findDSpaceObject(context, doc);
                } catch (SQLException ex) {
                    log.error(ex);
                    return;
                }
                try {
                    if (obj != null
                            && DryadWorkflowUtils.isAtLeastOneDataFileVisible(context, (Item) obj))
                    {
                        references.add(obj);
                        // dates.add(doc.getFieldValue(config.getString("recent.submissions.sort-option")).toString());
                        o = doc.getFieldValue(config.getString("recent.submissions.sort-option"));
                        if (o instanceof ArrayList) {
                            dates.add(((ArrayList) o).get(0).toString());
                        } else if (o instanceof String) {
                            dates.add(o.toString());
                        } else if (o instanceof Date) {
                            dates.add(fmt.format(o));
                        } else {
                            dates.add(o.toString());
                        }
                    }
                } catch (SQLException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        }
    }

    @Override
    public String getView() {
        return "site";
    }
    @Override
    public Serializable getKey() {
        // do not allow this to be cached
        return null;
    }
}
