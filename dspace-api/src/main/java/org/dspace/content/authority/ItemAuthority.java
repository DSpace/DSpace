/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;

/**
 * Sample authority to link a dspace item with another (i.e a publication with
 * the corresponding dataset or viceversa)
 *
 * @author Andrea Bollini
 * @version $Revision $
 */
public class ItemAuthority implements ChoiceAuthority
{
    private static final Logger log = Logger.getLogger(ItemAuthority.class);
    
    private DSpace dspace = new DSpace();
    
    private SearchService searchService  = dspace.getServiceManager().getServiceByName(
            "org.dspace.discovery.SearchService", SearchService.class);

    // punt!  this is a poor implementation..
    @Override
    public Choices getBestMatch(String field, String text, int collection, String locale)
    {
        return getMatches(field, text, collection, 0, 2, locale);
    }

    /**
	 * Match a proposed value against existend DSpace item applying an optional
	 * filter query to limit the scope only to specific item types
	 */
    @Override
    public Choices getMatches(String field, String text, int collection, int start, int limit, String locale)
    {
    	Context context = null;
    	if (limit <= 0) {
    		limit = 20;
    	}
    	
        String luceneQuery = ClientUtils.escapeQueryChars(text);
        luceneQuery = luceneQuery.replaceAll("\\\\ "," ");
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(org.dspace.core.Constants.ITEM);
        String filter = ConfigurationManager.getProperty("cris","ItemAuthority."
                + field + ".filter");
        if (StringUtils.isNotBlank(filter))
        {
            discoverQuery.addFilterQueries(filter);
        }

        discoverQuery
                .setQuery(luceneQuery);
        discoverQuery.setStart(start);
        discoverQuery.setMaxResults(limit);
        
        DiscoverResult resultSearch;
		try {
			context = new Context();
			resultSearch = searchService.search(context,
			        discoverQuery, false);
			List<Choice> choiceList = new ArrayList<Choice>();

	        for (DSpaceObject dso : resultSearch.getDspaceObjects())
	        {
	            choiceList.add(new Choice(dso.getHandle(), dso.getName(),  dso.getName()));
	        }

	        Choice[] results = new Choice[choiceList.size()];
	        results = choiceList.toArray(results);
			return new Choices(results, 0, results.length, Choices.CF_AMBIGUOUS,
					resultSearch.getTotalSearchResults() > (start + limit), 0);
	        
		} catch (SearchServiceException | SQLException e) {
			log.error(e.getMessage(), e);
			return new Choices(true);
		}
		finally {
			if (context != null && context.isValid()) {
				context.abort();
			}
		}
    }

    @Override
    public String getLabel(String field, String key, String locale)
    {
    	String title = key;
    	if (key != null) {
    		Context context = null;
	    	try {
	    		context = new Context();
	    		DSpaceObject dso = HandleManager.resolveToObject(context, key);
	    		if (dso != null) {
	    			title = dso.getName();
	    		}
	    	} catch (SQLException e) {
				log.error(e.getMessage(), e);
				return key;
			}
			finally {
				if (context != null && context.isValid()) {
					context.abort();
				}
			}
    	}
        return title;
    }
}
