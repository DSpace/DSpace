/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
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
import org.dspace.workflow.DryadWorkflowUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Transformer that displays the recently submitted items on the dspace home page
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SiteRecentSubmissions extends AbstractFiltersTransformer {

    private static final Logger log = Logger.getLogger(SiteRecentSubmissions.class);

    private static final Message T_head_recent_submissions =
            message("xmlui.ArtifactBrowser.SiteViewer.head_recent_submissions");


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

        Division home = body.addDivision("site-home", "primary repository");

        Division lastSubmittedDiv = home
                .addDivision("site-recent-submission", "secondary recent-submission");

        lastSubmittedDiv.setHead(T_head_recent_submissions);

        ReferenceSet lastSubmitted = lastSubmittedDiv.addReferenceSet(
                "site-last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                null, "recent-submissions");

        int numberOfItemsAdded=0;
        if (queryResults != null)  {
            String searchUrl="discover?sort_by=dc.date.issued_dt_sort&order=DESC&submit=Go";
            lastSubmittedDiv.addList("most_recent").addItemXref(searchUrl,"View more");
            for (SolrDocument doc : queryResults.getResults()) {
                DSpaceObject obj = SearchUtils.findDSpaceObject(context, doc);
                if(obj != null)
                {
                    // filter out Items that are not world-readable
                    if (!includeRestrictedItems) {
                        if (DryadWorkflowUtils.isAtLeastOneDataFileVisible(context, (Item)obj)) {
                            lastSubmitted.addReference(obj);
                            numberOfItemsAdded++;
                            if(numberOfItemsAdded==numberOfItemsToShow)
                                return;
                        }
                    }
                }
            }
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

}