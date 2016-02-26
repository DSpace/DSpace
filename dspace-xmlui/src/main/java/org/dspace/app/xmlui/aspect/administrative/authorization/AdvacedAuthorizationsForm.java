/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.authorization;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Alexey Maslov
 */
public class AdvacedAuthorizationsForm extends AbstractDSpaceTransformer
{	
	private static final Message T_title = 
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.title");
	private static final Message T_trail =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.trail");
	private static final Message T_authorize_trail =
		message("xmlui.administrative.authorization.general.authorize_trail");
	
	private static final Message T_main_head =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.main_head");
	private static final Message T_main_para =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.main_para");
	
	private static final Message T_actions_groupSentence =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_groupSentence");
    private static final Message T_actions_actionSentence =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_actionSentence");
    private static final Message T_actions_resourceSentence =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_resourceSentence");
    private static final Message T_actions_collectionSentence =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_collectionSentence");
    
	private static final Message T_actions_policyGroup =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_policyGroup");
    private static final Message T_actions_policyAction =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_policyAction");
    private static final Message T_actions_policyResource =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_policyResource");
    private static final Message T_actions_policyCollections =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_policyCollections");


    private static final Message T_actions_description =
            message("xmlui.administrative.authorization.AdvancedAuthorizationsForm.actions_description");
    private static final Message T_actions_start_date =
            message("xmlui.administrative.authorization.AdvancedAuthorizationsForm.actions_start_date");
    private static final Message T_actions_end_date =
            message("xmlui.administrative.authorization.AdvancedAuthorizationsForm.actions_end_date");
    
    private static final Message T_submit_add =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.submit_add");
    private static final Message T_submit_remove_all =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.submit_remove_all");
    private static final Message T_submit_return =
		message("xmlui.general.return");
	
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");

    private static final Message T_label_date_help =
            message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.label_date_help");


    private static final Message T_error_groupIds = message("xmlui.administrative.authorization.AdvancedAuthorizationsForm.error_groupIds");
    private static final Message T_error_collectionIds = message("xmlui.administrative.authorization.AdvancedAuthorizationsForm.error_collectionIds");
    private static final Message T_error_date_format = message("xmlui.administrative.authorization.AdvancedAuthorizationsForm.error_date_format");
    private static final Message T_error_start_date_greater_than_end_date = message("xmlui.administrative.authorization.AdvancedAuthorizationsForm.error_start_date_greater_than_end_date");
	
	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
	protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/authorize", T_authorize_trail);
        pageMeta.addTrail().addContent(T_trail);

        pageMeta.addMetadata("javascript", "static").addContent("static/js/advancedAuthorizationsForm.js");
    }
		
	public void addBody(Body body) throws WingException, SQLException 
	{
        int resourceID = parameters.getParameterAsInteger("resource_id", -1);
        int actionID = parameters.getParameterAsInteger("action_id",-1);
        String rpDescription = parameters.getParameter("description", null);
        String rpStartDate = parameters.getParameter("startDate", null);
        String rpEndDate = parameters.getParameter("endDate", null);

        Request request = ObjectModelHelper.getRequest(objectModel);

        String[] groupIDs = null;
        if(request.getAttribute("groupIDs")!=null){
            groupIDs = (String[])request.getAttribute("groupIDs");
        }

        String[] collectionIDs = null;
        if(request.getAttribute("collectionIDs")!=null){
            collectionIDs = (String[])request.getAttribute("collectionIDs");
        }

        ArrayList<String> errors = getListOfValues(parameters.getParameter("errors",null));

		Division main = body.addInteractiveDivision("advanced-authorization",contextPath+"/admin/authorize",Division.METHOD_POST,"primary administrative authorization");
		main.setHead(T_main_head);
		main.addPara(T_main_para);		
		
		
		List actionsList = main.addList("actions","form");


        // description
        Text description = actionsList.addItem().addText("description");
        description.setLabel(T_actions_description);
        description.setValue(rpDescription);

		// For all of the selected groups...
		actionsList.addItem().addContent(T_actions_groupSentence);
        actionsList.addLabel(T_actions_policyGroup);
        Select groupSelect = actionsList.addItem().addSelect("group_id");
        groupSelect.setMultiple(true);
        groupSelect.setSize(15);
        if (errors.contains("groupIDs")){
            groupSelect.addError(T_error_groupIds);
        }
        for (Group group : groupService.findAll(context, null))
        {
            if(wasElementSelected(group.getID().toString(), groupIDs)){
                groupSelect.addOption(true, group.getID().toString(), group.getName());
            }else{
                groupSelect.addOption(false, group.getID().toString(), group.getName());
            }
        }
        
        // Grant the ability to perform the following action...
        actionsList.addItem().addContent(T_actions_actionSentence);
        actionsList.addLabel(T_actions_policyAction);
        Select actionSelect = actionsList.addItem().addSelect("action_id");
        for( int i = 0; i < Constants.actionText.length; i++ )
        {
            if (actionID == i){
                actionSelect.addOption(true, i, Constants.actionText[i]);
            }else{
                actionSelect.addOption(i, Constants.actionText[i]);
            }
        }
        
        // For all following object types...
        actionsList.addItem().addContent(T_actions_resourceSentence);
        actionsList.addLabel(T_actions_policyResource);
        Select resourceSelect = actionsList.addItem().addSelect("resource_id");
        if(resourceID==Constants.BITSTREAM ){
            resourceSelect.addOption(false, Constants.ITEM, "item");
            resourceSelect.addOption(true, Constants.BITSTREAM, "bitstream");
        }else{ // default
            resourceSelect.addOption(true, Constants.ITEM, "item");
            resourceSelect.addOption(false, Constants.BITSTREAM, "bitstream");
        }
        
        // Across the following collections...
        actionsList.addItem().addContent(T_actions_collectionSentence);
        actionsList.addLabel(T_actions_policyCollections);
        Select collectionsSelect = actionsList.addItem().addSelect("collection_id");
        collectionsSelect.setMultiple(true);
        collectionsSelect.setSize(15);
        if (errors.contains("collectionIDs")){
            collectionsSelect.addError(T_error_collectionIds);
        }
        for (Collection collection : collectionService.findAll(context))
        {
            if(wasElementSelected(collection.getID().toString(), collectionIDs)){
                collectionsSelect.addOption(true, collection.getID().toString(), collectionService.getMetadata(collection, "name"));
            }else{
                collectionsSelect.addOption(false, collection.getID().toString(), collectionService.getMetadata(collection, "name"));
            }
        }

        // start date
        Text startDate = actionsList.addItem().addText("start_date");
        startDate.setLabel(T_actions_start_date);
        startDate.setHelp(T_label_date_help);
        startDate.setValue(rpStartDate);
        if (errors.contains("startDate"))
            startDate.addError(T_error_date_format);
        else if (errors.contains("startDateGreaterThenEndDate"))
            startDate.addError(T_error_start_date_greater_than_end_date);

        // end date
        Text endDate = actionsList.addItem().addText("end_date");
        endDate.setLabel(T_actions_end_date);
        endDate.setHelp(T_label_date_help);
        endDate.setValue(rpEndDate);
        if (errors.contains("endDate"))
            endDate.addError(T_error_date_format);
        
        
    	Para buttons = main.addPara();
    	buttons.addButton("submit_add").setValue(T_submit_add);
    	buttons.addButton("submit_remove_all").setValue(T_submit_remove_all);
    	buttons.addButton("submit_return").setValue(T_submit_return);
    	
		main.addHidden("administrative-continue").setValue(knot.getId());
   }

    private ArrayList<String> getListOfValues(String input) {
        ArrayList<String> values = new ArrayList<String>();
        if (input != null){
            Collections.addAll(values, input.split(","));
        }
        return values;
    }


    private boolean wasElementSelected(String element, String[] elements){

        if(elements!=null){
            for(String s : elements){
                if(s.equals(element))
                    return true;
            }
        }
        return false;
    }
}
