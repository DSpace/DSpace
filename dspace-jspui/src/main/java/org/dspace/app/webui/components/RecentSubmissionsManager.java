/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import org.apache.log4j.Logger;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.dspace.utils.DSpace;

/**
 * Class that obtains recent submissions to DSpace containers.
 * @author rdjones
 *
 */
public class RecentSubmissionsManager
{
	/** logger */
	private static Logger log = Logger.getLogger(RecentSubmissionsManager.class);
	
	/** DSpace context */
	private Context context;
	
	private String indexName = "bi_item";
	
	/**
	 * Construct a new RecentSubmissionsManager with the given DSpace context
	 * 
	 * @param context
	 */
	public RecentSubmissionsManager(Context context)
	{
		this.context = context;
	}

	/**
	 * Obtain the recent submissions from the given container object.  This
	 * method uses the configuration to determine which field and how many
	 * items to retrieve from the DSpace Object.
	 * 
	 * If the object you pass in is not a Community or Collection (e.g. an Item
	 * is a DSpaceObject which cannot be used here), an exception will be thrown
	 * 
	 * @param dso	DSpaceObject: Community, Collection or null for SITE
	 * @return		The recently submitted items
	 * @throws RecentSubmissionsException
	 */
	public RecentSubmissions getRecentSubmissions(DSpaceObject dso)
		throws RecentSubmissionsException
	{
		try
		{
			// get our configuration
			String source = ConfigurationManager.getProperty("recent.submissions.sort-option");
			String count = ConfigurationManager.getProperty("recent.submissions.count");
			
			// prep our engine and scope			
			BrowserScope bs = new BrowserScope(context);
			bs.setUserLocale(context.getCurrentLocale().getLanguage());
			BrowseIndex bi = null; 
			if("bi_item".equals(this.indexName)) {        
			    bi = BrowseIndex.getItemBrowseIndex();
			}
			else {
			    bi = BrowseIndex.getBrowseIndex(indexName);
			}
			
            boolean isMultilanguage = new DSpace()
                    .getConfigurationService()
                    .getPropertyAsType(
                            "discovery.browse.authority.multilanguage."
                                    + bi.getName(),
                            new DSpace()
                                    .getConfigurationService()
                                    .getPropertyAsType(
                                            "discovery.browse.authority.multilanguage",
                                            new Boolean(false)),
                            false);
            
            // gather & add items to the feed.
            BrowseEngine be = new BrowseEngine(context, isMultilanguage? 
                    bs.getUserLocale():null);
			
			// fill in the scope with the relevant gubbins
			bs.setBrowseIndex(bi);
			bs.setOrder(SortOption.DESCENDING);
			bs.setResultsPerPage(Integer.parseInt(count));
            if (dso != null)
            {
                bs.setBrowseContainer(dso);
            }
            for (SortOption so : SortOption.getSortOptions())
            {
                if (so.getName().equals(source))
                {
                    bs.setSortBy(so.getNumber());
                }
            }
			
			BrowseInfo results = be.browseMini(bs);
			
			IGlobalSearchResult[] items = results.getItemResults(context);
			
			RecentSubmissions rs = new RecentSubmissions(items);
			
			return rs;
		}
        catch (SortException se)
        {
            log.error("caught exception: ", se);
            throw new RecentSubmissionsException(se);
        }
		catch (BrowseException e)
		{
			log.error("caught exception: ", e);
			throw new RecentSubmissionsException(e);
		}
	}

    public String getIndexName()
    {
        return indexName;
    }

    public void setIndexName(String indexName)
    {
        this.indexName = indexName;
    }
	
}
