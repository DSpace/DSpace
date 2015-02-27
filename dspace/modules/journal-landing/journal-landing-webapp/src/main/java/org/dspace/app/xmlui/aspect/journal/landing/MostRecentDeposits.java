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
import org.apache.avalon.framework.parameters.ParameterException;
import org.xml.sax.SAXException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.aspect.discovery.AbstractFiltersTransformer;
import org.dspace.app.xmlui.utils.UIException;
import static org.dspace.app.xmlui.wing.AbstractWingTransformer.message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
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
    private static final Message T_date = message("xmlui.JournalLandingPage.MostRecentDeposits.date");

    private ArrayList<DSpaceObject> references;
    private ArrayList<String> dates;
    private String journalName;

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        try {
            this.journalName = parameters.getParameter(PARAM_JOURNAL_NAME);
        } catch (ParameterException ex) {
            log.error(ex);
        }
        if (journalName == null || journalName.length() == 0) return;

        Division mostRecent = body.addDivision(MOST_RECENT_DEPOSITS_DIV);
        mostRecent.setHead(T_mostRecent);

        Division items = mostRecent.addDivision(ITEMS);
        ReferenceSet refs = items.addReferenceSet(MOST_RECENT_DEPOSITS_REFS, "summaryList");

        Division count = mostRecent.addDivision(VALS);
        List vals = count.addList("date-count", List.TYPE_SIMPLE, "date-count");
        vals.setHead(T_date);

        references = new ArrayList<DSpaceObject>();
        dates = new ArrayList<String>();
        try {
            performSearch(null);
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }
        if (references.size() > 0) {
            for (DSpaceObject ref : references)
                refs.addReference(ref);
            for (String s : dates)
                vals.addItem(s);
        }
        references = null;
        dates = null;
    }

    /**
     * Search for recently accessioned data packages
     *
     * @param object: unused
     */
    @Override
    public void performSearch(DSpaceObject object) throws SearchServiceException, UIException {
        queryArgs = prepareDefaultFilters(getView());
        queryArgs.setQuery("DSpaceStatus:Archived AND search.resourcetype:" + Constants.ITEM + " AND prism.publicationName:\"" + journalName + "\"");
        queryArgs.add("fl", depositsDisplayHandle + "," + depositsDisplayField);
        queryArgs.setRows(displayCount);
        queryArgs.setSortField(depositsDisplaySortField, depositsDisplaySortOrder);
        SearchService service = (SearchService) getSearchService();
        try {
            queryResults = service.search(context, queryArgs);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        if (queryResults == null) {
            log.debug("Null query results for journa: " + journalName);
            return;
        }
        for (SolrDocument doc : queryResults.getResults()) {
            if (references.size() > displayCount) break;
            DSpaceObject dso = null;
            try {
                dso = SearchUtils.findDSpaceObject(context, doc);
            } catch (SQLException ex) {
                log.error(ex);
                return;
            }
            try {
                if (dso != null
                 && DryadWorkflowUtils.isAtLeastOneDataFileVisible(context, (Item) dso))
                {
                    references.add(dso);
                    Object o = doc.getFieldValue(depositsDisplayField);
                    if (o instanceof ArrayList) {
                        o = ((ArrayList) o).get(0);
                    }
                    if (o instanceof Date) {
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
