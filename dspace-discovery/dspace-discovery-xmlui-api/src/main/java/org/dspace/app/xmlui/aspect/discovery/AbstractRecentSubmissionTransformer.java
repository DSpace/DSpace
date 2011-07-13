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


    protected abstract String getView();

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

            queryArgs.addFilterQueries(SearchUtils.getDefaultFilters(getView()));
            queryArgs.setDSpaceObjectFilter(Constants.ITEM);

            queryArgs.setMaxResults(SearchUtils.getConfig().getInt("solr.recent-submissions.size", 5));

            String sortField = SearchUtils.getConfig().getString("recent.submissions.sort-option");
            if(sortField != null){
                queryArgs.setSortField(
                        sortField,
                        DiscoverQuery.SORT_ORDER.desc
                );
            }

            SearchService service = SearchUtils.getSearchService();
            queryResults = service.search(context, dso, queryArgs);
        }catch (SearchServiceException se){
            log.error("Caught SearchServiceException while retrieving recent submission for: " + getView(), se);
        }
    }

    @Override
    public void recycle() {
        queryResults = null;
        validity = null;
        super.recycle();
    }

}
