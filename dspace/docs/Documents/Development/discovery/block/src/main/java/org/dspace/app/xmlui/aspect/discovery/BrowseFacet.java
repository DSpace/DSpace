package org.dspace.app.xmlui.aspect.discovery;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
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
import java.util.Collections;

import org.dspace.discovery.ServiceFactory;
import org.dspace.discovery.SearchServiceException;

/**
 * User: mdiggory
 * Date: Sep 25, 2009
 * Time: 11:54:11 PM
 */
public class BrowseFacet extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(BrowseFacet.class);

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
                DSpaceValidity validity = new DSpaceValidity();

                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

                if (dso != null)
                {
                    // Add the actual collection;
                    validity.add(dso);
                }

                // add reciently submitted items, serialize solr query contents.
                QueryResponse response = getQueryResponse(dso);

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
    protected QueryResponse getQueryResponse(DSpaceObject scope) {


        Request request = ObjectModelHelper.getRequest(objectModel);

        if(queryResults != null)
            return queryResults;

        SolrQuery query = new SolrQuery();

        query.setQuery("search.resourcetype:2");

        query.setRows(1);//RECENT_SUBMISISONS);


        query.setSortField(
                ConfigurationManager.getProperty("recent.submissions.sort-option"),
                SolrQuery.ORDER.asc
        );


        //Set the default limit to 11
        //query.setFacetLimit(11);
        query.setFacetMinCount(1);
        query.setFacet(true);

        java.util.List<String> fields = Arrays.asList(ServiceFactory.getSearchService().getFacetFields());

        String field = request.getParameter("field");
        if(field != null && fields.contains(field))
        {
            fields = Collections.singletonList(field);
        }

        query.addFacetField(fields.toArray(new String[0]));

        if (scope != null) /* top level search / community */
        {
            if (scope instanceof Community) {
                query.setFilterQueries("location:m" + scope.getID());
            } else if (scope instanceof Collection) {
                query.setFilterQueries("location:l" + scope.getID());
            }
        }


        try {
            queryResults =  ServiceFactory.getSearchService().search(query);
        } catch (SearchServiceException e) {
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



    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        java.util.List fqs = Arrays.asList(
                request.getParameterValues("fq") != null ? request.getParameterValues("fq") : new String[0]);

        // Set up the major variables
        //Collection collection = (Collection) dso;

        // Build the collection viewer division.
        Division home = body.addDivision("test", "primary repository");

        String name = "Browse ...."                ;
        //if (name == null || name.length() == 0)
        //	home.setHead(T_untitled);
        //else
        	home.setHead(name);


        if (this.queryResults != null) {

            java.util.List<FacetField> facetFields = this.queryResults.getFacetFields();

            if (facetFields != null && facetFields.size() > 0) {

                Division browse = home.addDivision("test2","secondary");

//                List browse = browseContainer.addList("discovery");

                browse.setHead("Filter By:");          /** TODO externalize */

                for (FacetField field : facetFields) {

                    java.util.List<FacetField.Count> values = field.getValues();

                    if (values != null) {

                        Iterator<FacetField.Count> iter = values.iterator();

                        List facet = browse.addList(field.getName(), null, "columns2");

                        facet.setHead(message("xmlui.ArtifactBrowser.AdvancedSearch.type_" + field.getName().replace("_lc","")));

                        while(iter.hasNext())//for (int i = 0; i < 10 ; i++)
                        {

                            FacetField.Count value = iter.next();

                            if(fqs.contains(value.getAsFilterQuery()))
                            {
                               facet.addItem().addContent(value.getName() + " (" + value.getCount() + ")");
                            }
                            else
                            {
                               facet.addItem().addXref(
                                    contextPath + /* "/handle/" + community.getHandle() + */
                                    "/search?" +
                                     ((dso != null) ? "scope=" + dso.getHandle() : "" )+
                                     "&fq=" +
                                     value.getAsFilterQuery(),
                                    value.getName() + " (" + value.getCount() + ")"
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