/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.*;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Renders a list of recently submitted items for the collection by using the dspace browse
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class CollectionRecentSubmissions extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(CollectionRecentSubmissions.class);

    /** How many recent submissions to include in the page */
    private static final int RECENT_SUBMISSIONS = 5;

    /** The cache of recently submitted items */
    private java.util.List<BrowseItem> recentSubmissionItems;

    private static final Message T_head_recent_submissions =
        message("xmlui.ArtifactBrowser.CollectionViewer.head_recent_submissions");

    /** Cached validity object */
    private SourceValidity validity;

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
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
     * The validity object will include the collection being viewed and
     * all recently submitted items. This does not include the community / collection
     * hierarchy, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity()
    {
    	if (this.validity == null)
    	{
            Collection collection = null;
	        try
	        {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

	            if (dso == null)
                {
                    return null;
                }

	            if (!(dso instanceof Collection))
                {
                    return null;
                }

	            collection = (Collection) dso;

	            DSpaceValidity validity = new DSpaceValidity();

	            // Add the actual collection;
	            validity.add(collection);

	            // add recently submitted items
	            for(BrowseItem item : getRecentlySubmittedItems(collection))
	            {
	                validity.add(item);
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
    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Collection))
        {
            return;
        }

        // Set up the major variables
        Collection collection = (Collection) dso;


        java.util.List<BrowseItem> items = getRecentlySubmittedItems(collection);
        if(items.size() == 0)
        {
            return;
        }
        
        Division home = body.addDivision("collection-home", "primary repository collection");
        
        Division lastSubmittedDiv = home
                .addDivision("collection-recent-submission","secondary recent-submission");
        lastSubmittedDiv.setHead(T_head_recent_submissions);
        ReferenceSet lastSubmitted = lastSubmittedDiv.addReferenceSet(
                "collection-last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                null, "recent-submissions");
        for (BrowseItem item : items)
        {
            lastSubmitted.addReference(item);
        }
    }

    /**
     * Get the recently submitted items for the given collection.
     *
     * @param collection The collection.
     */
    @SuppressWarnings("unchecked") // The cast from getLastSubmitted is correct, it dose infact return a list of Items.
    private java.util.List<BrowseItem> getRecentlySubmittedItems(Collection collection)
        throws SQLException
    {
        if (recentSubmissionItems != null)
        {
            return recentSubmissionItems;
        }

        String source = ConfigurationManager.getProperty("recent.submissions.sort-option");
        int numRecentSubmissions = ConfigurationManager.getIntProperty("recent.submissions.count", RECENT_SUBMISSIONS);
        if(numRecentSubmissions == 0)
        {
            return new ArrayList<BrowseItem>();
        }
        BrowserScope scope = new BrowserScope(context);
        scope.setCollection(collection);
        scope.setResultsPerPage(numRecentSubmissions);

        // FIXME Exception Handling
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
        	this.recentSubmissionItems = be.browse(scope).getResults();
        }
        catch (SortException se)
        {
            log.error("Caught SortException", se);
        }
        catch (BrowseException bex)
        {
        	log.error("Caught BrowseException", bex);
        }

        return this.recentSubmissionItems;
    }

    /**
     * Recycle
     */
    public void recycle()
    {
        // Clear out our item's cache.
        this.recentSubmissionItems = null;
        this.validity = null;
        super.recycle();
    }
}
