package edu.tamu.dspace.profilepagetweaks;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.xml.sax.SAXException;

public class ProfilePageTweaks extends AbstractDSpaceTransformer 
{
	protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
	protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
	protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    
    /** Determine if the user is registering for the first time */
    private String registering;
    private String epersonID;
    
    // A list of groups that this user is apart of.
    private List<Group> memberGroups = null;

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
    		EPerson eperson = ePersonService.find(context, UUID.fromString(epersonID));
    		memberGroups = groupService.allMemberGroups(context, eperson);
    	}
    	else if (!"true".equals(registering)) {
            memberGroups = groupService.allMemberGroups(context, context.getCurrentUser());
    	}
    	
    	if (memberGroups == null)
    		return;
        
        for (Group memberGroup : memberGroups) {
            String[] name = memberGroup.getName().split("_");
            if (name.length >= 3 && ("COLLECTION".equals(name[0]) || "COMMUNITY".equals(name[0])))
            {
                DSpaceObject dso;
                if ("COLLECTION".equals(name[0])) {
                	dso = collectionService.findByGroup(context, memberGroup);
                } else {
                    dso = communityService.findByAdminGroup(context, memberGroup);
                }
                if (dso != null) {
                	pageMeta.addMetadata(memberGroup.getName(), dso.getName()).addContent(dso.getHandle());
                }
            }
        }
        
    }
}
