package org.tamu.dspace.extensions;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.AbstractWingTransformer;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

public class RestrictedItemsTransformer extends AbstractWingTransformer 
{
    private List<String> restrictedOther;
    private List<String> restrictedCampus;
    
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    // Set up the restricted bitstreams listing
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
	
	// Grab the DSO from the object model
	try 
	{
	    DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	    Context context = ContextUtil.obtainContext(objectModel);
	    restrictedOther = new ArrayList<String>();
	    restrictedCampus = new ArrayList<String>();
	    
	    // Check to make sure it's an item
	    if (dso instanceof Item) 
	    {
		//AuthorizeManager.authorizeActionBoolean(context, dso, Constants.READ);
	
		
		Item item = (Item)dso;
		List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
		
		// If so, iterate over its bundles and bitstreams, taking note of any that we can't read
		for (Bundle bundle : bundles) 
		{
		    List<Bitstream> bitstreams = bundle.getBitstreams();
		    for (Bitstream bitstream : bitstreams)
		    {
				if (!authorizeService.authorizeActionBoolean(context, bitstream, Constants.READ)) {
					
					// The user can not read the bitstream, but why? Is
					// it because it is restricted to on campus access
					// only, or is it limited to logged in users?
					boolean oncampus = false;
					List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, bitstream, Constants.READ);
					for (ResourcePolicy policy :policies) {
						Group group = policy.getGroup();
						if (group == null)
							continue;
						if ("member".equals(group.getName()))
							oncampus = true;
					}
					
					if (oncampus) {
						restrictedCampus.add(bitstream.getName()+"?sequence="+bitstream.getSequenceID());
					} else {
						restrictedOther.add(bitstream.getName()+"?sequence="+bitstream.getSequenceID());
					}
				}
		    }
		}
		
	    }
	    
	} catch (SQLException e) {
	    throw new ProcessingException(e);
	}
	
	
	// Initialize the Wing framework.
        try
        {
            this.setupWing();
        }
        catch (WingException we)
        {
            throw new ProcessingException(we);
        }
	
    }
    
    
    // Go through an add a pageMeta element for each restricted bitstream
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
    	for (String restricted : restrictedOther)
    	{
    		pageMeta.addMetadata("restricted", "other").addContent(restricted);
    	}
    	for (String restricted : restrictedCampus)
    	{
    		pageMeta.addMetadata("restricted", "campus").addContent(restricted);
    	}
    }
}
