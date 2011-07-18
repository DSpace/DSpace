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
import org.dspace.core.Constants;
import org.dspace.discovery.SearchUtils;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Renders a list of recently submitted items for the collection by using discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class CollectionRecentSubmissions extends AbstractFiltersTransformer {

    private static final Logger log = Logger.getLogger(CollectionRecentSubmissions.class);
    
    private static final Message T_head_recent_submissions =
            message("xmlui.ArtifactBrowser.CollectionViewer.head_recent_submissions");


    /**
     * Display a single collection
     */
    public void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        // Set up the major variables
        Collection collection = (Collection) dso;

        performSearch(collection);

        if(queryResults == null)
        {
            return;
        }// queryResults;

        // Build the collection viewer division.
        Division home = body.addDivision("collection-home", "primary repository collection");

        Division lastSubmittedDiv = home
                .addDivision("collection-recent-submission", "secondary recent-submission");

        lastSubmittedDiv.setHead(T_head_recent_submissions);

        ReferenceSet lastSubmitted = lastSubmittedDiv.addReferenceSet(
                "collection-last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                null, "recent-submissions");

        for (SolrDocument doc : queryResults.getResults()) {
            lastSubmitted.addReference(
                    SearchUtils.findDSpaceObject(context, doc));
        }

    }


    /**
     * Get the recently submitted items for the given community or collection.
     *
     * @param scope The comm/collection.
     * @return the response of the query
     */
    public void performSearch(DSpaceObject scope) {
        if(queryResults != null)
        {
            return;
        }// queryResults;

        queryArgs = prepareDefaultFilters(getView());

        queryArgs.setQuery("search.resourcetype:" + Constants.ITEM);

        queryArgs.setRows(SearchUtils.getConfig().getInt("solr.recent-submissions.size", 5));

        String sortField = SearchUtils.getConfig().getString("recent.submissions.sort-option");
        if(sortField != null){
            queryArgs.setSortField(
                    sortField,
                    SolrQuery.ORDER.desc
            );
        }

        queryArgs.setFilterQueries("location:l" + scope.getID());

        try {
            queryResults =  getSearchService().search(queryArgs);
        } catch (RuntimeException e) {
            log.error(e.getMessage(),e);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
    }

    public String getView()
    {
        return "collection";
    }
}
