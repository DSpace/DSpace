/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResult.FacetResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.utils.DSpace;

/**
 * Discovery (Solr) driver implementing ItemCountDAO interface to look up item
 * count information in communities and collections. Caching operations are
 * intentionally not implemented because Solr already is our cache.
 * 
 * @author Ivan Masár, Andrea Bollini
 * 
 */
public class ItemCountDAOSolr implements ItemCountDAO
{
    /** Log4j logger */
    private static Logger log = Logger.getLogger(ItemCountDAOSolr.class);
    
    /** DSpace context */
    private Context context;
    
    /**
     * Hold the communities item count obtained from SOLR after the first query. This only works
     * well if the ItemCountDAO lifecycle is bound to the request lifecycle as
     * it is now. If we switch to a Spring-based instantiation we should mark
     * this bean as prototype
     **/
    private Map<String, Integer> communitiesCount = null;

    /** Hold the collection item count obtained from SOLR after the first query **/
    private Map<String, Integer> collectionsCount = null;
    
    /** DSpace helper services access object */
    DSpace dspace = new DSpace();
    
    /** Solr search service */
    SearchService searcher = dspace.getServiceManager().getServiceByName(SearchService.class.getName(), SearchService.class);
    
    /**
     * Throw an ItemCountException as caching is not supported by ItemCountDAOSolr.
     * 
     * @param collection
     * @param count
     * @throws ItemCountException
     */
    public void collectionCount(Collection collection, int count) throws ItemCountException
    {
        throw new ItemCountException("Caching is not supported by the ItemCountDAOSolr as it is not really needed, Solr is faster!");
    }

    /**
     * Throw an ItemCountException as caching is not supported by ItemCountDAOSolr.
     * 
     * @param community
     * @param count
     * @throws ItemCountException
     */
    public void communityCount(Community community, int count) throws ItemCountException
    {
        throw new ItemCountException("Caching is not supported by the ItemCountDAOSolr as it is not really needed, Solr is faster!");
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
    	loadCount();
    	DiscoverQuery query = new DiscoverQuery();
    	Integer val = null;
    	if (dso instanceof Collection)
        {
            val = collectionsCount.get(String.valueOf(((Collection) dso).getID()));
        }
        else if (dso instanceof Community)
        {
            val = communitiesCount.get(String.valueOf(((Community) dso).getID()));
        }
        else
        {
            throw new ItemCountException("We can only count items in Communities or Collections");
        }
        
    	if (val != null)
    	{
            return val.intValue();
    	}
    	else
    	{
            return 0;
    	}
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
     * make sure that the counts are actually fetched from Solr (if haven't been
     * cached in a Map yet)
     * 
     * @throws ItemCountException
     */
    private void loadCount() throws ItemCountException
    {
    	if (communitiesCount != null || collectionsCount != null)
    	{
            return;
    	}
    	
    	communitiesCount = new HashMap<String, Integer>();
        collectionsCount = new HashMap<String, Integer>();
        
        DiscoverQuery query = new DiscoverQuery();
        query.setFacetMinCount(1);
        query.addFacetField(new DiscoverFacetField("location.comm",
                            DiscoveryConfigurationParameters.TYPE_STANDARD, -1,
                            DiscoveryConfigurationParameters.SORT.COUNT));
        query.addFacetField(new DiscoverFacetField("location.coll",
                            DiscoveryConfigurationParameters.TYPE_STANDARD, -1,
                            DiscoveryConfigurationParameters.SORT.COUNT));
        query.addFilterQueries("search.resourcetype:2");    // count only items
        query.addFilterQueries("NOT(discoverable:false)");  // only discoverable
        query.setMaxResults(0);
        
        DiscoverResult sResponse = null;
        try
        {
            sResponse = searcher.search(context, query, false);
            List<FacetResult> commCount = sResponse.getFacetResult("location.comm");
            List<FacetResult> collCount = sResponse.getFacetResult("location.coll");
            for (FacetResult c : commCount)
            {
            	communitiesCount.put(c.getAsFilterQuery(),(int) c.getCount());
            }
            for (FacetResult c : collCount)
            {
            	collectionsCount.put(c.getAsFilterQuery(),(int) c.getCount());
            }
        }
        catch (SearchServiceException e)
        {
            log.error("caught exception: ", e);
            throw new ItemCountException(e);
        }
    }
}
