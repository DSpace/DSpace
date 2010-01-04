package org.dspace.app.xmlui.aspect.discovery;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.authorize.AuthorizeException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.excalibur.source.SourceValidity;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.Serializable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.net.URLEncoder;

import org.dspace.discovery.ServiceFactory;
import org.dspace.discovery.SearchServiceException;

/**
 * User: mdiggory
 * Date: Sep 25, 2009
 * Time: 11:54:11 PM
 */
public class AbstractRecentSubmissions extends AbstractDSpaceTransformer implements CacheableProcessingComponent {
    private static final Logger log = Logger.getLogger(CollectionRecentSubmissions.class);
    /**
     * How many recent submissions to include in the page
     */
    private static final int RECENT_SUBMISISONS = 5;
    /**
     * The cache of recently submitted items
     */
    protected QueryResponse queryResults;
    /**
     * Cached validity object
     */
    protected SourceValidity validity;

    /**
     * Cached query arguments
     */
    protected SolrQuery queryArgs;

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
                return "0";

            return HashUtil.hash(dso.getHandle());
        }
        catch (SQLException sqle) {
            // Ignore all errors and just return that the component is not
            // cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * <p/>
     * The validity object will include the collection being viewed and
     * all recently submitted items. This does not include the community / collection
     * hierarch, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity() {
        if (this.validity == null) {

            try {
                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

                if (dso == null)
                    return null;

                DSpaceValidity validity = new DSpaceValidity();

                // Add the actual collection;
                validity.add(dso);

                // add reciently submitted items, serialize solr query contents.
                QueryResponse response = getRecentlySubmittedItems(dso);

                validity.add("numFound:" + response.getResults().getNumFound());

                for (SolrDocument doc : response.getResults()) {
                    validity.add(doc.toString());
                }

                for (SolrDocument doc : response.getResults()) {
                    validity.add(doc.toString());
                }

                for(FacetField field : response.getFacetFields())
                {
                    validity.add(field.getName());

                    for(FacetField.Count count : field.getValues())
                    {
                        validity.add(count.getName() + count.getCount());
                    }
                }
               

                this.validity = validity.complete();
            }
            catch (Exception e) {
                // Just ignore all errors and return an invalid cache.
            }

            //TODO: dependent on tags as well :)
        }
        return this.validity;
    }

    /**
     * Get the recently submitted items for the given community or collection.
     *
     * @param scope The collection.
     */
    protected QueryResponse getRecentlySubmittedItems(DSpaceObject scope) {


        if(queryResults != null)
            return queryResults;

        queryArgs = new SolrQuery();

        queryArgs.setQuery("search.resourcetype:2");

        queryArgs.setRows(RECENT_SUBMISISONS);

        queryArgs.setSortField(
                ConfigurationManager.getProperty("recent.submissions.sort-option"),
                SolrQuery.ORDER.asc
        );

        int max = ConfigurationManager.getIntProperty("search.facet.max",10) + 1;

        //Set the default limit to 11
        queryArgs.setFacetLimit(max);

        queryArgs.setFacetMinCount(1);
        queryArgs.setFacet(true);

        queryArgs.addFacetField(ServiceFactory.getSearchService().getFacetFields());
        
        if (scope != null) /* top level search / community */
        {
            if (scope instanceof Community) {
                queryArgs.setFilterQueries("location:m" + scope.getID());
            } else if (scope instanceof Collection) {
                queryArgs.setFilterQueries("location:l" + scope.getID());
            }
        }


        try {
            queryResults =  ServiceFactory.getSearchService().search(queryArgs);
        } catch (Throwable e) {
            log.error(e.getMessage(),e);
        }

        return queryResults;
    }


        /**
     * Add a page title and trail links.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
                WingException, UIException, SQLException, IOException,
                AuthorizeException {

    }



    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        java.util.List fqs = Arrays.asList(
                request.getParameterValues("fq") != null ? request.getParameterValues("fq") : new String[0]);

        if (this.queryResults != null) {
            java.util.List<FacetField> facetFields = this.queryResults.getFacetFields();

            if (facetFields != null && facetFields.size() > 0) {

                List browse = options.addList("discovery");

                browse.setHead("Filter By:");          /** TODO externalize */

                for (FacetField field : facetFields) {

                    java.util.List<FacetField.Count> values = field.getValues();


                    if (values != null) {

                        Iterator<FacetField.Count> iter = values.iterator();

                        List facet = browse.addList(field.getName());

                        facet.setHead(message("xmlui.ArtifactBrowser.AdvancedSearch.type_" + field.getName().replace("_lc","")));

                        for (int i = 0; i < this.queryArgs.getFacetLimit() ; i++)
                        {

                            if(!iter.hasNext())
                                break;

                            FacetField.Count value = iter.next();

                            if(i < this.queryArgs.getFacetLimit() - 1)
                            {
                                 if(fqs.contains(value.getAsFilterQuery()))
                                {
                                    facet.addItem().addContent(value.getName() + " (" + value.getCount() + ")");
                                }
                                else
                                {
                                    facet.addItem().addXref(
                                    contextPath + /* "/handle/" + community.getHandle() + */
                                    "/search?" +
                                     request.getQueryString() +
                                     "&fq=" +
                                     URLEncoder.encode(value.getAsFilterQuery(),"UTF-8"),
                                    value.getName() + " (" + value.getCount() + ")"
                                    );
                                }
                            }
                            else
                            {

                                facet.addItem().addXref(
                                    contextPath + /* "/handle/" + community.getHandle() + */
                                    "/browse?field=" + field.getName()  +
                                     (request.getQueryString() != null ? "&" + request.getQueryString() : "" ),
                                     "... View More"

                                );
                            }
                        }
                    }


                }
            }
        }

        //DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        /*
        if (dso != null)
        {
            if (dso instanceof Collection)
            {
                browseContext.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2+AND+location%3Al" + dso.getID(), T_head_this_collection );
            }
            if (dso instanceof Community)
            {
                browseContext.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2+AND+location%3Am" + dso.getID(), T_head_this_community );
            }
        }

        browseGlobal.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2", T_head_all_of_dspace );
        */
    }

    /**
     * Recycle
     */
    public void recycle() {
        // Clear out our item's cache.
        this.queryResults = null;
        this.validity = null;
        super.recycle();
    }
}
