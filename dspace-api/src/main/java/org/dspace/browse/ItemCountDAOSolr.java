/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

/**
 * Discovery (Solr) driver implementing ItemCountDAO interface to look up item
 * count information in communities and collections. Caching operations are
 * intentionally not implemented because Solr already is our cache.
 * 
 * @author Ivan Masár
 * 
 */
public class ItemCountDAOSolr implements ItemCountDAO
{
    /** Log4j logger */
    private static Logger log = Logger.getLogger(ItemCountDAOSolr.class);
    
    /** DSpace context */
    private Context context;
    
    /** DSpace helper services access object */
    DSpace dspace = new DSpace();
    
    /** Solr search service */
    SearchService searcher = dspace.getServiceManager().getServiceByName(SearchService.class.getName(), SearchService.class);
    
    /**
     * Store the count of the given collection (does nothing in the Solr backend).
     * 
     * @param collection
     * @param count
     * @throws ItemCountException
     */
    public void collectionCount(Collection collection, int count) throws ItemCountException
    {
    }

    /**
     * Store the count of the given community (does nothing in the Solr backend).
     * 
     * @param community
     * @param count
     * @throws ItemCountException
     */
    public void communityCount(Community community, int count) throws ItemCountException
    {
    }

    /**
     * Set the dspace context to use
     * 
     * @param context
     * @throws ItemCountException
     */
    public void setContext(Context context) throws ItemCountException
    {
        this.context = context;
    }

    /**
     * Get the count of the items in the given container.
     * 
     * @param dso
     * @throws ItemCountException
     */
    public int getCount(DSpaceObject dso) throws ItemCountException
    {
            DiscoverQuery query = new DiscoverQuery();
            if (dso instanceof Collection)
            {
                query.addFilterQueries("location.coll:" + ((Collection) dso).getID());
            }
            else if (dso instanceof Community)
            {
                query.addFilterQueries("location.comm:" + ((Community) dso).getID());
            }
            else
            {
                throw new ItemCountException("We can only count items in Communities or Collections");
            }
            query.addFilterQueries("search.resourcetype:2");    // count only items
            query.addFilterQueries("NOT(discoverable:false)");    // only discoverable
            query.setMaxResults(0);
            
            DiscoverResult sResponse = null;
            try
            {
                sResponse = searcher.search(context, query, false);
            }
            catch (SearchServiceException e)
            {
                log.error("caught exception: ", e);
                throw new ItemCountException(e);
            }
            return (int) sResponse.getTotalSearchResults();
    }

    /**
     * remove the cache for the given container (does nothing in the Solr backend)
     * 
     * @param dso
     * @throws ItemCountException
     */
    public void remove(DSpaceObject dso) throws ItemCountException
    {
    }

    /**
     * remove the cache for the given collection (does nothing in the Solr backend)
     * 
     * @param collection
     * @throws ItemCountException
     */
    private void removeCollection(Collection collection) throws ItemCountException
    {
    }
    
    /**
     * Remove the cache for the given community (does nothing in the Solr backend)
     * 
     * @param community
     * @throws ItemCountException
     */
    private void removeCommunity(Community community) throws ItemCountException
    {
    }
    
}
