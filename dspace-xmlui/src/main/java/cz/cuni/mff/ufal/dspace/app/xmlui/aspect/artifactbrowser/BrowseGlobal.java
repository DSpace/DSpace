/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;


public class BrowseGlobal extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
	/** Language Strings */
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    private static final Message T_head_browse =
        message("xmlui.ArtifactBrowser.CollectionViewer.head_browse");

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() 
    {
       return "1";
    }

    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity() 
    {
        return NOPValidity.SHARED_INSTANCE;
    }
    
    /**
     * Add a page title and trail links.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
    	pageMeta.addMetadata("title").addContent(T_dspace_home);
    	pageMeta.addTrailLink(contextPath, T_dspace_home);
        
        // Add RSS links if available
        String formats = ConfigurationManager.getProperty("webui.feed.formats");
		if ( formats != null )
		{
			for (String format : formats.split(","))
			{
				// Remove the protocol number, i.e. just list 'rss' or' atom'
				String[] parts = format.split("_");
				if (parts.length < 1)
                {
                    continue;
                }
				
				String feedFormat = parts[0].trim()+"+xml";
					
				String feedURL = contextPath+"/feed/"+format.trim()+"/site";
				pageMeta.addMetadata("feed", feedFormat).addContent(feedURL);
			}
		}
    }

    /**
     * Display a single collection
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	
     // Browse by list
    	Division div = body.addDivision("browse-top", "primary");
    	div.setHead("Browse the Repository by"); 
        div.addPara("You can browse the repository based on the following fields.");
        Division browseDiv = div.addDivision("collection-browse","secondary browse");
        List browse = browseDiv.addList("collection-browse", List.TYPE_SIMPLE,
                "collection-browse");	
        try
        {
            // Get a Map of all the browse tables
            BrowseIndex[] bis = BrowseIndex.getBrowseIndices();
            for (BrowseIndex bix : bis)
            {
                // Create a Map of the query parameters for this link
                Map<String, String> queryParams = new HashMap<String, String>();

                queryParams.put("type", bix.getName());

                // Add a link to this browse
                browse.addItemXref(generateURL(contextPath + "/browse", queryParams),
                        message("xmlui.ArtifactBrowser.Navigation.browse_" + bix.getName()));
            }
        }
        catch (BrowseException bex)
        {
            throw new UIException("Unable to get browse indicies", bex);
        }

    }
}

