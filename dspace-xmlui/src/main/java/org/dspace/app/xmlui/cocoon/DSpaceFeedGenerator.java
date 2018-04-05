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

import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.FeedUtils;
import org.dspace.app.util.SyndicationFeed;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowserScope;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sun.syndication.io.FeedException;

/**
 *
 * Generate a syndication feed for DSpace, either a community or collection
 * or the whole repository. This code was adapted from the syndication found
 * in DSpace's JSP implementation, "org.dspace.app.webui.servlet.FeedServlet".
 *
 * One thing that has been modified from DSpace's JSP implementation is what
 * is placed inside an item's description, we've changed it so that the list
 * of metadata fields is scanned until a value is found and the first one
 * found is used as the description. This means that we look at the abstract,
 * description, alternative title, title, etc., too and the first metadata
 * value found is used.
 *
 * I18N: Feeds are internationalized, meaning that they may contain references
 * to messages contained in the global messages.xml file using cocoon's i18n
 * schema. However, the library I used to build the feeds does not understand
 * this schema to work around this limitation, so I created a little hack. It
 * basically works like this, when text that needs to be localized is put into
 * the feed it is always mangled such that a prefix is added to the messages's
 * key. Thus if the key were "xmlui.feed.text" then the resulting text placed
 * into the feed would be "I18N:xmlui.feed.text". After the library is finished
 * and produced, its final result the output is traversed to find these
 * occurrences ande replace them with proper cocoon i18n elements.
 *
 *
 *
 * @author Scott Phillips, Ben Bosman, Richard Rodgers
 */

public class DSpaceFeedGenerator extends AbstractGenerator
                implements Configurable, CacheableProcessingComponent, Recyclable
{
    private static final Logger log = Logger.getLogger(DSpaceFeedGenerator.class);

    /** The feed's requested format */
    private String format = null;
    
    /** The feed's scope, null if no scope */
    private String handle = null;
    
    /** number of DSpace items per feed */
    private static final int ITEM_COUNT = DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("webui.feed.items");
    
    /**
     * How long should RSS feed cache entries be valid? milliseconds * seconds *
     * minutes * hours default to 24 hours if config parameter is not present or
     * wrong
     */
    private static final long CACHE_AGE;
    static
    {
        final String ageCfgName = "webui.feed.cache.age";
        final long ageCfg = DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty(ageCfgName, 24);
        CACHE_AGE = 1000 * 60 * 60 * ageCfg;
    }
    
    /** configuration option to include Item which does not have READ by Anonymous enabled **/
    private static boolean includeRestrictedItems = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("harvest.includerestricted.rss", true);


    /** Cache of this object's validitity */
    private DSpaceValidity validity = null;
    
    /** The cache of recently submitted items */
    private List<Item> recentSubmissionItems;

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
        String key = "key:" + this.handle + ":" + this.format;
        return HashUtil.hash(key);
    }

    /**
     * Generate the cache validity object.
     *
     * The validity object will include the collection being viewed and
     * all recently submitted items. This does not include the community / collection
     * hierarchy, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity()
    {
        if (this.validity == null)
        {
            try
            {
                DSpaceValidity validity = new FeedValidity();
                
                Context context = ContextUtil.obtainContext(objectModel);

                DSpaceObject dso = null;
                
                if (handle != null && !handle.contains("site"))
                {
                    dso = handleService.resolveToObject(context, handle);
                }
                
                validity.add(context, dso);
                
                // add recently submitted items
                for(Item item : getRecentlySubmittedItems(context,dso))
                {
                    validity.add(context, item);
                }

                this.validity = validity.complete();
            }
            catch (RuntimeException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                return null;
            }
        }
        return this.validity;
    }
    
    
    
    /**
     * Setup component wide configuration
     */
    public void configure(Configuration conf) throws ConfigurationException
    {
    }
    
    
    /**
     * Setup configuration for this request
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, par);
        
        this.format = par.getParameter("feedFormat", null);
        this.handle = par.getParameter("handle",null);
    }
    
    
    /**
     * Generate the syndication feed.
     */
    public void generate() throws IOException, SAXException, ProcessingException
    {
        try
        {
            Context context = ContextUtil.obtainContext(objectModel);
            DSpaceObject dso = null;
            
            if (handle != null && !handle.contains("site"))
            {
                dso = handleService.resolveToObject(context, handle);
                if (dso == null)
                {
                    // If we were unable to find a handle then return page not found.
                    throw new ResourceNotFoundException("Unable to find DSpace object matching the given handle: "+handle);
                }
                
                if (!(dso.getType() == Constants.COLLECTION || dso.getType() == Constants.COMMUNITY))
                {
                    // The handle is valid but the object is not a container.
                    throw new ResourceNotFoundException("Unable to syndicate DSpace object: "+handle);
                }
            }
        
            SyndicationFeed feed = new SyndicationFeed(SyndicationFeed.UITYPE_XMLUI);
            feed.populate(ObjectModelHelper.getRequest(objectModel), context,
                          dso, getRecentlySubmittedItems(context,dso), FeedUtils.i18nLabels);
            feed.setType(this.format);
            Document dom = feed.outputW3CDom();
            FeedUtils.unmangleI18N(dom);
            DOMStreamer streamer = new DOMStreamer(contentHandler, lexicalHandler);
            streamer.stream(dom);
        }
        catch (IllegalArgumentException iae)
        {
                throw new ResourceNotFoundException("Syndication feed format, '"+this.format+"', is not supported.", iae);
        }
        catch (FeedException fe)
        {
                throw new SAXException(fe);
        }
        catch (SQLException sqle)
        {
                throw new SAXException(sqle);
        }
    }
    
    /**
     * @return recently submitted Items within the indicated scope
     */
    @SuppressWarnings("unchecked")
    private List<Item> getRecentlySubmittedItems(Context context, DSpaceObject dso)
            throws SQLException
    {
        if (recentSubmissionItems != null)
        {
            return recentSubmissionItems;
        }

        String source = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("recent.submissions.sort-option");
        BrowserScope scope = new BrowserScope(context);
        if (dso instanceof Collection)
        {
            scope.setCollection((Collection) dso);
        }
        else if (dso instanceof Community)
        {
            scope.setCommunity((Community) dso);
        }
        scope.setResultsPerPage(ITEM_COUNT);

        // FIXME Exception handling
        try
        {
            scope.setBrowseIndex(BrowseIndex.getItemBrowseIndex());
            for (SortOption so : SortOption.getSortOptions())
            {
                if (so.getName().equals(source))
                {
                    scope.setSortBy(so.getNumber());
                    scope.setOrder(SortOption.DESCENDING);
                }
            }

            BrowseEngine be = new BrowseEngine(context);
            List<Item> browseItemResults = be.browseMini(scope).getBrowseItemResults();
            this.recentSubmissionItems = browseItemResults;

            // filter out Items that are not world-readable
            if (!includeRestrictedItems)
            {
                List<Item> result = new ArrayList<Item>();
                for (Item item : this.recentSubmissionItems)
                {
                checkAccess:
                    for (Group group : authorizeService.getAuthorizedGroups(context, item, Constants.READ))
                    {
                        if ((group.getName().equals(Group.ANONYMOUS)))
                        {
                            result.add(item);
                            break checkAccess;
                        }
                    }
                }
                this.recentSubmissionItems = result;
            }
        }
        catch (BrowseException bex)
        {
            log.error("Caught browse exception", bex);
        }
        catch (SortException e)
        {
            log.error("Caught sort exception", e);
        }
        return this.recentSubmissionItems;
    }
    
    /**
     * Recycle
     */
    
    public void recycle()
    {
        this.format = null;
        this.handle = null;
        this.validity = null;
        this.recentSubmissionItems = null;
        super.recycle();
    }
    
    /**
     * Extend the standard DSpaceValidity object to support assumed
     * caching. Since feeds will constantly be requested we want to
     * assume that a feed is still valid instead of checking it
     * against the database anew everytime.
     *
     * This validity object will assume that a cache is still valid,
     * without rechecking it, for 24 hours.
     *
     */
    private static class FeedValidity extends DSpaceValidity
    {
        private static final long serialVersionUID = 1L;
                        
        /** When the cache's validity expires */
        private long expires = 0;
        
        /**
         * When the validity is completed record a timestamp to check later.
         */
        public DSpaceValidity complete()
        {
                this.expires = System.currentTimeMillis() + CACHE_AGE;
                
                return super.complete();
        }
        
        
        /**
         * Determine if the cache is still valid
         */
        public int isValid()
        {
            // Return true if we have a hash.
            if (this.completed)
            {
                if (System.currentTimeMillis() < this.expires)
                {
                        // If the cache hasn't expired the just assume that it is still valid.
                        return SourceValidity.VALID;
                }
                else
                {
                        // The cache is past its age
                        return SourceValidity.UNKNOWN;
                }
            }
            else
            {
                // This is an error state. We are being asked whether we are valid before
                // we have been initialized.
                return SourceValidity.INVALID;
            }
        }

        /**
         * Determine if the cache is still valid based
         * upon the other validity object.
         *
         * @param other
         *          The other validity object.
         */
        public int isValid(SourceValidity otherValidity)
        {
            if (this.completed && otherValidity instanceof FeedValidity)
            {
                FeedValidity other = (FeedValidity) otherValidity;
                if (hash == other.hash)
                {
                    // Update expiration time of both caches.
                    this.expires = System.currentTimeMillis() + CACHE_AGE;
                    other.expires = System.currentTimeMillis() + CACHE_AGE;

                    return SourceValidity.VALID;
                }
            }

            return SourceValidity.INVALID;
        }

    }
}
