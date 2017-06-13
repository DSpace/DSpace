/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.AbstractGenerator;
import org.apache.cocoon.util.HashUtil;
import org.apache.cocoon.xml.dom.DOMStreamer;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.aspect.discovery.AbstractFiltersTransformer;
import org.dspace.app.xmlui.aspect.discovery.CollectionRecentSubmissions;
import org.dspace.app.xmlui.aspect.discovery.FilterSearchUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.FeedUtils;
import org.dspace.app.util.SyndicationFeed;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowserScope;
import org.dspace.content.*;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.DryadWorkflowUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sun.syndication.io.FeedException;

import javax.mail.MethodNotSupportedException;

/**
 *
 * Generate a syndication feed for DSpace, either a community or collection
 * or the whole repository. This code was adapted from the syndication found
 * in DSpace's JSP implementation, "org.dspace.app.webui.servlet.FeedServlet".
 *
 * Once thing that has been modified from DSpace's JSP implementation is what
 * is placed inside an item's description, we've changed it so that the list
 * of metadata fields is scanned until a value is found and the first one
 * found is used as the description. This means that we look at the abstract,
 * description, alternative title, title, etc... to and the first metadata
 * value found is used.
 *
 * I18N: Feed's are internationalized, meaning that they may contain refrences
 * to messages contained in the global messages.xml file using cocoon's i18n
 * schema. However the library used to build the feeds does not understand
 * this schema to work around this limitation I created a little hack. It
 * basicaly works like this, when text that needs to be localized is put into
 * the feed it is allways mangled such that a prefix is added to the messages's
 * key. Thus if the key were "xmlui.feed.text" then the resulting text placed
 * into the feed would be "I18N:xmlui.feed.text". After the library is finished
 * and produced it's final result the output is traversed to find these
 * occurances ande replace them with proper cocoon i18n elements.
 *
 *
 *
 * @author Scott Phillips, Ben Bosman, Richard Rodgers
 */


/**
 * Overriding reasons:
 * <p/>
 * - check if the datafiles contained in the datapackage are accessible
 * if a datafile is under embargo don't add the dataPackage to the result list.
 */

public class DSpaceFeedGenerator extends AbstractGenerator
        implements Configurable, CacheableProcessingComponent, Recyclable {
    private static final Logger log = Logger.getLogger(DSpaceFeedGenerator.class);

    /**
     * The feed's requested format
     */
    private String format = null;

    /**
     * The feed's scope, null if no scope
     */
    private String handle = null;

    /**
     * number of DSpace items per feed
     */
    private static final int ITEM_COUNT = ConfigurationManager.getIntProperty("webui.feed.items");

    /**
     * How long should RSS feed cache entries be valid? milliseconds * seconds *
     * minutes * hours default to 24 hours if config parameter is not present or
     * wrong
     */
    private static final long CACHE_AGE;

    static {
        final String ageCfgName = "webui.feed.cache.age";
        final long ageCfg = ConfigurationManager.getIntProperty(ageCfgName, 24);
        CACHE_AGE = 1000 * 60 * 60 * ageCfg;
    }

    /**
     * configuration option to include Item which does not have READ by Anonymous enabled *
     */
    private static boolean includeRestrictedItems = ConfigurationManager.getBooleanProperty("harvest.includerestricted.rss", false);


    /**
     * Cache of this object's validitity
     */
    private DSpaceValidity validity = null;

    /**
     * The cache of recently submitted items
     */
    private Item recentSubmissionItems[];

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        String key = "key:" + this.handle + ":" + this.format;
        return HashUtil.hash(key);
    }

    /**
     * Generate the cache validity object.
     * <p/>
     * The validity object will include the collection being viewed and
     * all recently submitted items. This does not include the community / collection
     * hierarch, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity() {

        log.debug("DSpaceFeedGenerator - getValidity() !!!!!");

        if (this.validity == null) {
            try {
                //DSpaceValidity validity = new FeedValidity();
                DSpaceValidity validity = new DSpaceValidity();

                Context context = ContextUtil.obtainContext(objectModel);

                DSpaceObject dso = null;

                if (handle != null && !handle.contains("site")) {
                    dso = HandleManager.resolveToObject(context, handle);
                }

                validity.add(dso);

                // add recently submitted items
                for (Item item : getRecentlySubmittedItems(context, dso)) {
                    validity.add(item);
                }

                this.validity = validity.complete();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                return null;
            }
        }
        return this.validity;
    }


    /**
     * Setup component wide configuration
     */
    public void configure(Configuration conf) throws ConfigurationException {
        log.debug("DSpaceFeedGenerator - configure() !!!!!");
    }


    /**
     * Setup configuration for this request
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
                      Parameters par) throws ProcessingException, SAXException,
            IOException {
        super.setup(resolver, objectModel, src, par);

        this.format = par.getParameter("feedFormat", null);
        this.handle = par.getParameter("handle", null);
    }


    /**
     * Generate the syndication feed.
     */
    public void generate() throws IOException, SAXException, ProcessingException {

        log.debug("DSpaceFeedGenerator - generate() !!!!!");

        try {
            Context context = ContextUtil.obtainContext(objectModel);
            DSpaceObject dso = null;

            if (handle != null && !handle.contains("site")) {
                dso = HandleManager.resolveToObject(context, handle);
                if (dso == null) {
                    // If we were unable to find a handle then return page not found.
                    throw new ResourceNotFoundException("Unable to find DSpace object matching the given handle: " + handle);
                }

                if (!(dso.getType() == Constants.COLLECTION || dso.getType() == Constants.COMMUNITY)) {
                    // The handle is valid but the object is not a container.
                    throw new ResourceNotFoundException("Unable to syndicate DSpace object: " + handle);
                }
            }

            SyndicationFeed feed = new SyndicationFeed(SyndicationFeed.UITYPE_XMLUI);


            log.debug("generate() !!!!!! : calling  getRecentlySubmittedItemsUsingDiscovery(context, dso);");

            feed.populate(ObjectModelHelper.getRequest(objectModel), dso, getRecentlySubmittedItemsUsingDiscovery(context, dso), FeedUtils.i18nLabels);
            feed.setType(this.format);
            Document dom = feed.outputW3CDom();
            FeedUtils.unmangleI18N(dom);
            DOMStreamer streamer = new DOMStreamer(contentHandler, lexicalHandler);
            streamer.stream(dom);
        } catch (IllegalArgumentException iae) {
            throw new ResourceNotFoundException("Syndication feed format, '" + this.format + "', is not supported.", iae);
        } catch (FeedException fe) {
            throw new SAXException(fe);
        } catch (SQLException sqle) {
            throw new SAXException(sqle);
        }
    }

    /**
     * @return recently submitted Items within the indicated scope
     */
    @SuppressWarnings("unchecked")
    private Item[] getRecentlySubmittedItems(Context context, DSpaceObject dso)
            throws SQLException {

        log.debug("getRecentlySubmittedItems() !!!!!! : recentSubmissionItems ==>> " + recentSubmissionItems);

        if (recentSubmissionItems != null) {
            return recentSubmissionItems;
        }


        log.debug("getRecentlySubmittedItems() !!!!!! : calling  getRecentlySubmittedItemsUsingDiscovery(context, dso);");

        return getRecentlySubmittedItemsUsingDiscovery(context, dso);
    }



    /**
     * Recycle
     */

    public void recycle() {
        this.format = null;
        this.handle = null;
        this.validity = null;
        this.recentSubmissionItems = null;
        super.recycle();
    }





    private Item[] getRecentlySubmittedItemsUsingDiscovery(Context context, DSpaceObject dso) throws SQLException {


        log.debug("getRecentlySubmittedItemsUsingDiscovery(Context context, DSpaceObject dso)!!!!! ");

        QueryResponse queryResults = performSearch(context, dso);

        log.debug("getRecentlySubmittedItemsUsingDiscovery(Context context, DSpaceObject dso) - queryResults: " + queryResults);

        List<Item> result = new ArrayList<Item>();

        int numberOfItemsToShow = ConfigurationManager.getIntProperty("webui.feed.items");

        int numberOfItemsAdded=0;
        for (SolrDocument doc : queryResults.getResults()) {
            Item item = (Item) SearchUtils.findDSpaceObject(context, doc);
            if (!includeRestrictedItems){
                if (DryadWorkflowUtils.isAtLeastOneDataFileVisible(context, item)) {
                    result.add(item);
                    numberOfItemsAdded++;
                    if(numberOfItemsToShow==numberOfItemsAdded){
                        break;
                    }

                }
            }

        }
        this.recentSubmissionItems = result.toArray(new Item[result.size()]);
        return recentSubmissionItems;
    }


    private QueryResponse performSearch(Context context, DSpaceObject scope) throws SQLException {
        QueryResponse queryResults = null;


        FilterSearchUtil fs = new FilterSearchUtil(context);
        SolrQuery queryArgs = fs.prepareDefaultFilters(getView(scope));

        queryArgs.setQuery("search.resourcetype:" + Constants.ITEM);
        queryArgs.setRows(1000);
        String sortField = SearchUtils.getConfig().getString("recent.submissions.sort-option");

        if (sortField != null) {
            queryArgs.setSortField(
                    sortField,
                    SolrQuery.ORDER.desc
            );
        }

        try {

            queryResults = fs.getSearchService().search(context, queryArgs);

        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return queryResults;
    }


    private String getView(DSpaceObject dso) {
        return "site";
    }
}
