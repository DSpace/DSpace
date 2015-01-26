/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.aspect.discovery.AbstractFiltersTransformer;
import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.workflow.DryadWorkflowUtils;
import org.xml.sax.SAXException;

/**
 *
 * @author Nathan Day
 */
public class JournalLandingTabbedTransformer extends AbstractFiltersTransformer {
    
    private static final Logger log = Logger.getLogger(TopTenDownloads.class);
   
    // cocoon parameters
    protected String journalName;
        
    // performSearch() values
    private ArrayList<DSpaceObject> references;
    private ArrayList<String> values;
    private String q;
    private String sortOption;
    private String sortFieldOption;
    private TabData currentTabData;
    
    // config values (final)
    private final String submissionSize = "solr.recent-submissions.size";
    private final int submissionSizeDefault = 5;
    private final String includeRestricted = "harvest.includerestricted.rss";
    private final boolean includeRestrictedDefault = false;
    
    // container for data pertaining to entire div
    protected class DivData {
        public String sortOption;
        public String sortFieldOption;
        public String n;
        public Message T_div_head;
    }
    protected DivData divData;
    protected class TabData {
        public String n;
        public Message buttonLabel;
        public String query;
        public Message refHead;
        public Message valHead;
    }
    protected ArrayList<TabData> tabData;
    
    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, parameters);
        try {
            journalName = parameters.getParameter(PARAM_JOURNAL_NAME);
        } catch (ParameterException ex) {
            log.error(ex);
            throw new ProcessingException(ex.getMessage());
        }
    }
    
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division outer = body.addDivision(divData.n,divData.n);
        outer.setHead(divData.T_div_head);

        // tab buttons
        List tablist = outer.addList(TABLIST, List.TYPE_ORDERED, TABLIST);
        for(TabData t : tabData) {
            tablist.addItem(t.buttonLabel);
        }        
        sortOption = divData.sortOption;
        sortFieldOption = divData.sortFieldOption;
        for(TabData t : tabData) {
            q = t.query;
            Division wrapper = outer.addDivision(t.n, t.n);
            Division items = wrapper.addDivision(ITEMS);
            // reference list
            ReferenceSet refs = items.addReferenceSet(t.n, ReferenceSet.TYPE_SUMMARY_LIST);
            refs.setHead(t.refHead);
            // dspace item value list
            Division count = wrapper.addDivision(VALS);
            List list = count.addList(t.n, List.TYPE_SIMPLE, t.n);
            list.setHead(t.valHead);

            references = new ArrayList<DSpaceObject>();
            values = new ArrayList<String>();
            try {
                performSearch(null);
            } catch (SearchServiceException e) {
                log.error(e.getMessage(), e);
            }
            if (references.isEmpty()) {
                list.addItem(EMPTY_VAL);
            } else {
                for (DSpaceObject ref : references)
                    refs.addReference(ref);
                for (String s : values)
                    list.addItem(s);
            }
        }
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
        queryArgs.setQuery(q);
        queryArgs.setRows(10);
        String sortField = SearchUtils.getConfig().getString(sortFieldOption);
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
        boolean includeRestrictedItems = ConfigurationManager.getBooleanProperty(includeRestricted,includeRestrictedDefault);
        int numberOfItemsToShow= SearchUtils.getConfig().getInt(submissionSize, submissionSizeDefault);
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
                        Object o = doc.getFieldValue(config.getString(sortOption));
                        if (o instanceof ArrayList) {
                            values.add(((ArrayList) o).get(0).toString());
                        } else if (o instanceof String) {
                            values.add(o.toString());
                        }

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
