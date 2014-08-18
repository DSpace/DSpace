package edu.tamu.dspace.profilepagetweaks;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Field;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.Subscribe;
import org.xml.sax.SAXException;

public class ProfilePageTweaks extends AbstractDSpaceTransformer 
{
    
    /** Determine if the user is registering for the first time */
    private String registering;
    private String epersonID;
    
    // A list of groups that this user is apart of.
    private Group[] memberships = null;

    public void setup(SourceResolver resolver, Map objectModel, String src,
                      Parameters parameters) throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver,objectModel,src,parameters);
        
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        this.registering = request.getParameter("registering");
        this.epersonID = request.getParameter("epersonID");
    }
    
    
    // Go through an add a pageMeta element for each restricted bitstream
    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException
    {
    	// First, figure out which page we're on: eperson view or profile
    	if (epersonID != null) {
    		EPerson eperson = EPerson.find(context, Integer.parseInt(epersonID));
    		memberships = Group.allMemberGroups(context, eperson);
    	}
    	else if (!"true".equals(registering)) {
            memberships = Group.allMemberGroups(context, context.getCurrentUser());
    	}
    	
    	if (memberships == null)
    		return;
        
        for (Group member : memberships) {
            String[] name = member.getName().split("_");
            if (name.length >= 3 && ("COLLECTION".equals(name[0]) || "COMMUNITY".equals(name[0])))
            {
                DSpaceObject dso;
                if ("COLLECTION".equals(name[0]))
                    dso = Collection.find(context, Integer.parseInt(name[1]));
                else
                    dso = Community.find(context, Integer.parseInt(name[1]));
                    
                pageMeta.addMetadata(member.getName(), dso.getName()).addContent(dso.getHandle());
            }
        }
        
    }
}
