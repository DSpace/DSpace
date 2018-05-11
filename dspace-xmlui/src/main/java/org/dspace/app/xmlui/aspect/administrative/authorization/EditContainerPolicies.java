/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.authorization;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Highlight;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;

/**
 * @author Alexey Maslov
 */
public class EditContainerPolicies extends AbstractDSpaceTransformer   
{	
	private static final Message T_title = 
		message("xmlui.administrative.authorization.EditContainerPolicies.title");
	private static final Message T_policyList_trail =
		message("xmlui.administrative.authorization.general.policyList_trail");
	private static final Message T_authorize_trail =
		message("xmlui.administrative.authorization.general.authorize_trail");
	
	private static final Message T_main_head_collection =
		message("xmlui.administrative.authorization.EditContainerPolicies.main_head_collection");
	private static final Message T_main_head_community =
		message("xmlui.administrative.authorization.EditContainerPolicies.main_head_community");
	
	private static final Message T_add_link =
		message("xmlui.administrative.authorization.EditContainerPolicies.main_add_link");
	
	private static final Message T_head_id =
		message("xmlui.administrative.authorization.EditContainerPolicies.head_id");
	private static final Message T_head_action =
		message("xmlui.administrative.authorization.EditContainerPolicies.head_action");
	private static final Message T_head_group =
		message("xmlui.administrative.authorization.EditContainerPolicies.head_group");
	private static final Message T_group_edit =
		message("xmlui.administrative.authorization.EditContainerPolicies.group_edit");
	
	private static final Message T_submit_delete =
		message("xmlui.administrative.authorization.EditContainerPolicies.submit_delete");
	private static final Message T_submit_return =
		message("xmlui.general.return");
	
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();


		
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/authorize", T_authorize_trail);
        pageMeta.addTrail().addContent(T_policyList_trail);
        
    }
		
	public void addBody(Body body) throws WingException, SQLException 
	{
		/* Get and setup our parameters */
        int containerType = parameters.getParameterAsInteger("containerType",-1);
        UUID containerID = UUID.fromString(parameters.getParameter("containerID", null));
        int highlightID = parameters.getParameterAsInteger("highlightID",-1);
        String baseURL = contextPath+"/admin/epeople?administrative-continue="+knot.getId();
        
        ArrayList<ResourcePolicy> policies;

        // DIVISION: edit-container-policies
        Division main = body.addInteractiveDivision("edit-container-policies",contextPath+"/admin/authorize",Division.METHOD_POST,"primary administrative authorization");
		
		if (containerType == Constants.COLLECTION)
	    {
			Collection col = collectionService.find(context, containerID);
			main.setHead(T_main_head_collection.parameterize(collectionService.getMetadata(col, "name"),col.getHandle(),col.getID()));
			policies = (ArrayList<ResourcePolicy>) authorizeService.getPolicies(context, col);
	    }
		else 
		{
			Community com = communityService.find(context, containerID);
			main.setHead(T_main_head_community.parameterize(communityService.getMetadata(com, "name"), com.getHandle(), com.getID()));
			policies = (ArrayList<ResourcePolicy>) authorizeService.getPolicies(context, com);
		}
		
		/* Adding a new policy link */
		main.addPara().addXref(baseURL + "&submit_add", T_add_link);
		
    	Table table = main.addTable("container-policy-list",policies.size() + 1, 4);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell();
        header.addCell().addContent(T_head_id);
        header.addCell().addContent(T_head_action);
        header.addCell().addContent(T_head_group);

        if (policies != null)
        {
            for (ResourcePolicy policy : policies)
            {
                Row row;
                if (policy.getID() == highlightID)
                {
                    row = table.addRow(null, null, "highlight");
                }
                else
                {
                    row = table.addRow();
                }

                CheckBox select = row.addCell().addCheckBox("select_policy");
                select.setLabel(String.valueOf(policy.getID()));
                select.addOption(String.valueOf(policy.getID()));

                // Accounting for the funky case of an empty policy
                Group policyGroup = policy.getGroup();

                row.addCell().addXref(baseURL + "&submit_edit&policy_id=" + policy.getID(), String.valueOf(policy.getID()));
                row.addCell().addXref(baseURL + "&submit_edit&policy_id=" + policy.getID(), resourcePolicyService.getActionText(policy));
                if (policyGroup != null) {
                    Cell groupCell = row.addCell();
                    groupCell.addContent(policyGroup.getName());
                    Highlight groupHigh = groupCell.addHighlight("fade");
                    groupHigh.addContent(" [");
                    groupHigh.addXref(baseURL + "&submit_edit_group&group_id=" + policyGroup.getID(), T_group_edit);
                    groupHigh.addContent("]");
                }
                else {
                    row.addCell().addContent("...");
                }
            }
        }
        
    	Para buttons = main.addPara();
    	buttons.addButton("submit_delete").setValue(T_submit_delete);
    	buttons.addButton("submit_return").setValue(T_submit_return);
    	
		main.addHidden("administrative-continue").setValue(knot.getId());
   }
}
