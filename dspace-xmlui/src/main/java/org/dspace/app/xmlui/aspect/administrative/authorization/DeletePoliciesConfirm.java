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

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;

/**
 * @author Alexey Maslov
 */
public class DeletePoliciesConfirm extends AbstractDSpaceTransformer   
{	
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	private static final Message T_title =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.title");
	private static final Message T_trail =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.trail");
	private static final Message T_authorize_trail =
		message("xmlui.administrative.authorization.general.authorize_trail");
	private static final Message T_policyList_trail =
		message("xmlui.administrative.authorization.general.policyList_trail");
	
	private static final Message T_confirm_head =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.confirm_head");
	private static final Message T_confirm_para =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.confirm_para");
	
	private static final Message T_head_id =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.head_id");
	private static final Message T_head_action =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.head_action");
	private static final Message T_head_group =
		message("xmlui.administrative.authorization.DeletePoliciesConfirm.head_group");
	
	private static final Message T_submit_confirm =
		message("xmlui.general.delete");
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");

	protected ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/authorize", T_authorize_trail);
        pageMeta.addTrail().addContent(T_policyList_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
		
	public void addBody(Body body) throws WingException, SQLException 
	{
		// Get all our parameters
		String idsString = parameters.getParameter("policyIDs", null);
		
		ArrayList<ResourcePolicy> policies = new ArrayList<ResourcePolicy>();
		for (String id : idsString.split(","))
		{
			ResourcePolicy policy = resourcePolicyService.find(context,Integer.valueOf(id));
			policies.add(policy);
		}
 
		// DIVISION: policies-confirm-delete
    	Division deleted = body.addInteractiveDivision("policies-confirm-delete",contextPath+"/admin/authorize",Division.METHOD_POST,"primary administrative authorization");
    	deleted.setHead(T_confirm_head);
    	deleted.addPara(T_confirm_para);
    	
    	Table table = deleted.addTable("policies-confirm-delete",policies.size() + 1, 4);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_head_id);
        header.addCell().addContent(T_head_action);
        header.addCell().addContent(T_head_group);
    	
    	for (ResourcePolicy policy : policies) 
    	{
    		Row row = table.addRow();
    		row.addCell().addContent(policy.getID());
        	row.addCell().addContent(resourcePolicyService.getActionText(policy));
        	if (policy.getGroup() != null)
            {
                row.addCell().addContent(policy.getGroup().getName());
            }
        	else
            {
                row.addCell().addContent("...");
            }
	    }
    	Para buttons = deleted.addPara();
    	buttons.addButton("submit_confirm").setValue(T_submit_confirm);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    	
    	deleted.addHidden("administrative-continue").setValue(knot.getId());
   }
}
