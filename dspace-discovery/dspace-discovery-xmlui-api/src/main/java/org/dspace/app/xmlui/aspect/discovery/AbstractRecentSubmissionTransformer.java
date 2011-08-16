/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryRecentSubmissionsConfiguration;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

/**
 * An abstract class containing the shared methods which all recent submission transformers use
 *
 *  @author Kevin Van de Velde (kevin at atmire dot com)
 */
public abstract class AbstractRecentSubmissionTransformer extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(AbstractRecentSubmissionTransformer.class);

    /**
     * Cached query results
     */
    protected DiscoverResult queryResults;

    /** Cached validity object */
    private SourceValidity validity;

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    @Override
    public Serializable getKey() {
        try
        {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
            {
                return "0";
            }

            return HashUtil.hash(dso.getHandle());
        }
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not
            // cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     *
     * The validity object all recently submitted items.
     * This does not include the community / collection
     * hierarchy, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity()
    {
    	if (this.validity == null)
    	{
	        try
	        {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

	            DSpaceValidity validity = new DSpaceValidity();

	            // Add the actual collection;
	            validity.add(dso);

                getRecentlySubmittedItems(dso);
                if(queryResults != null){
                    List<DSpaceObject> resultingObjects = queryResults.getDspaceObjects();
                    for(DSpaceObject resultObject : resultingObjects){
                        validity.add(resultObject);
                    }
                    validity.add("numFound:" + resultingObjects.size());
                }

	            this.validity = validity.complete();
	        }
	        catch (Exception e)
	        {
	            // Just ignore all errors and return an invalid cache.
	        }
    	}
    	return this.validity;
    }

    /**
     * Retrieves the recent submitted items of the given scope
     *
     * @param dso the DSpace object can either be null (indicating home page), a collection or a community
     */
    protected void getRecentlySubmittedItems(DSpaceObject dso) {

        if(queryResults != null)
        {
            return; // queryResults;
        }

        try {
            DiscoverQuery queryArgs = new DiscoverQuery();

            //Add the default filter queries
            DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(dso);
            List<String> defaultFilterQueries = discoveryConfiguration.getDefaultFilterQueries();
            queryArgs.addFilterQueries(defaultFilterQueries.toArray(new String[defaultFilterQueries.size()]));
            queryArgs.setDSpaceObjectFilter(Constants.ITEM);

            DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration = discoveryConfiguration.getRecentSubmissionConfiguration();
            if(recentSubmissionConfiguration != null){
                queryArgs.setMaxResults(recentSubmissionConfiguration.getMax());
                String sortField = SearchUtils.getSearchService().toSortFieldIndex(recentSubmissionConfiguration.getMetadataSortField(), recentSubmissionConfiguration.getType());
                if(sortField != null){
                    queryArgs.setSortField(
                            sortField,
                            DiscoverQuery.SORT_ORDER.desc
                    );
                }
                SearchService service = SearchUtils.getSearchService();
                queryResults = service.search(context, dso, queryArgs);
            }else{
                //No configuration, no results
                queryResults = null;
            }
        }catch (SearchServiceException se){
            log.error("Caught SearchServiceException while retrieving recent submission for: " + (dso == null ? "home page" : dso.getHandle()), se);
        }
    }

    @Override
    public void recycle() {
        queryResults = null;
        validity = null;
        super.recycle();
    }

}
