/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.authorization;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * @author Alexey Maslov
 */
public class EditPolicyForm extends AbstractDSpaceTransformer
{
    private static final Message T_title =
            message("xmlui.administrative.authorization.EditPolicyForm.title");
    private static final Message T_trail =
            message("xmlui.administrative.authorization.EditPolicyForm.trail");
    private static final Message T_authorize_trail =
            message("xmlui.administrative.authorization.general.authorize_trail");
    private static final Message T_policyList_trail =
            message("xmlui.administrative.authorization.general.policyList_trail");

    private static final Message T_main_head_new =
            message("xmlui.administrative.authorization.EditPolicyForm.main_head_new");
    private static final Message T_main_head_edit =
            message("xmlui.administrative.authorization.EditPolicyForm.main_head_edit");


    private static final Message T_error_no_group =
            message("xmlui.administrative.authorization.EditPolicyForm.error_no_group");
    private static final Message T_error_no_action =
            message("xmlui.administrative.authorization.EditPolicyForm.error_no_action");


    private static final Message T_label_date_help =
            message("xmlui.administrative.authorization.EditPolicyForm.label_date_help");


    private static final Message T_no_results =
            message("xmlui.administrative.group.EditGroupForm.no_results");
    private static final Message T_groups_column1 =
            message("xmlui.administrative.authorization.EditPolicyForm.groups_column1");
    private static final Message T_groups_column2 =
            message("xmlui.administrative.authorization.EditPolicyForm.groups_column2");
    private static final Message T_groups_column3 =
            message("xmlui.administrative.authorization.EditPolicyForm.groups_column3");
    private static final Message T_groups_column4 =
            message("xmlui.administrative.authorization.EditPolicyForm.groups_column4");

    private static final Message T_submit_save =
            message("xmlui.general.save");
    private static final Message T_submit_cancel =
            message("xmlui.general.cancel");

    private static final Message T_set_group =
            message("xmlui.administrative.authorization.EditPolicyForm.set_group");
    private static final Message T_current_group =
            message("xmlui.administrative.authorization.EditPolicyForm.current_group");
    private static final Message T_groups_head =
            message("xmlui.administrative.authorization.EditPolicyForm.groups_head");
    private static final Message T_policy_currentGroup =
            message("xmlui.administrative.authorization.EditPolicyForm.policy_currentGroup");
    private static final Message T_label_search =
            message("xmlui.administrative.authorization.EditPolicyForm.label_search");
    private static final Message T_submit_search_groups =
            message("xmlui.administrative.authorization.EditPolicyForm.submit_search_groups");
    private static final Message T_label_action =
            message("xmlui.administrative.authorization.EditPolicyForm.label_action");


    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");


    // new fields for Restricted
    private static final Message T_label_name =
            message("xmlui.administrative.authorization.EditPolicyForm.label_name");
    private static final Message T_label_description =
            message("xmlui.administrative.authorization.EditPolicyForm.label_description");
    private static final Message T_label_start_date =
            message("xmlui.administrative.authorization.EditPolicyForm.label_start_date");
    private static final Message T_label_end_date =
            message("xmlui.administrative.authorization.EditPolicyForm.label_end_date");
    private static final Message T_error_date_format =
            message("xmlui.administrative.authorization.EditPolicyForm.error_date_format");
    private static final Message T_error_start_date_greater_than_end_date =
            message("xmlui.administrative.authorization.EditPolicyForm.error_start_date_greater_than_end_date");
    private static final Message T_error_duplicated_policy =
            message("xmlui.administrative.authorization.EditPolicyForm.error_duplicated_policy");

   	protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
   	protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
   	protected ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();


    // How many search results are displayed at once
    private static final int RESULTS_PER_PAGE = 10;

    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/authorize", T_authorize_trail);
        pageMeta.addTrail().addContent(T_policyList_trail);
        pageMeta.addTrail().addContent(T_trail);

        pageMeta.addMetadata("javascript", "static").addContent("static/js/editPolicyForm.js");
    }

    public void addBody(Body body) throws WingException, SQLException
    {
        /* Get and setup our parameters. We should always have an objectType and objectID, since every policy
           * has to have a parent resource. policyID may, however, be -1 for new, not-yet-created policies and
           * the groupID is only set if the group is being changed. */
        int objectType = parameters.getParameterAsInteger("objectType",-1);
        String objectID = parameters.getParameter("objectID", null);
        int policyID = parameters.getParameterAsInteger("policyID",-1);
        String groupID = parameters.getParameter("groupID", null);
        int actionID = parameters.getParameterAsInteger("actionID",-1);
        int page = parameters.getParameterAsInteger("page",0);
        String query = decodeFromURL(parameters.getParameter("query","-1"));
        String rpName = parameters.getParameter("name", null);
        String rpDescription = parameters.getParameter("description", null);
        String rpStartDate = parameters.getParameter("startDate", null);
        String rpEndDate = parameters.getParameter("endDate", null);

        // The current policy, if it exists (i.e. we are not creating a new one)
        ResourcePolicy policy = resourcePolicyService.find(context, policyID);

        if (policy != null){
            if(rpName==null || rpName.equals(""))
                rpName = policy.getRpName();

            if(rpDescription==null || rpDescription.equals(""))
                rpDescription = policy.getRpDescription();

            if(StringUtils.isBlank(rpStartDate) && policy.getStartDate()!=null){
                rpStartDate = DateFormatUtils.format(policy.getStartDate(), "yyyy-MM-dd");
            }

            if(StringUtils.isBlank(rpEndDate) && policy.getEndDate() != null){
                rpEndDate = DateFormatUtils.format(policy.getEndDate(), "yyyy-MM-dd");
            }
        }

        // The currently set group; it's value depends on wether previously clicked the "Set" button to change 
        // the associated group, came here to edit an existing group, or create a new one. 
        Group currentGroup;
        if (StringUtils.isNotBlank(groupID)) {
            currentGroup = groupService.find(context, UUID.fromString(groupID));
        }
        else if (policy != null) {
            currentGroup = policy.getGroup();
        }
        else
        {
            currentGroup = null;
        }

        // Same for the current action; it can either blank (-1), manually set, or inherited from the current policy
        if (policy != null && actionID == -1)
        {
            actionID = policy.getAction();


        }



        String errorString = parameters.getParameter("errors",null);
        ArrayList<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }


        /* Set up our current Dspace object */
        DSpaceObject dso = null;
        if(objectID != null){
            dso = ContentServiceFactory.getInstance().getDSpaceObjectService(objectType).find(context, UUID.fromString(objectID));
        }

        // DIVISION: edit-container-policies
        Division main = body.addInteractiveDivision("edit-policy",contextPath+"/admin/authorize",Division.METHOD_POST,"primary administrative authorization");

        if (policyID >= 0) {
            objectID = policy.getdSpaceObject().getID().toString();
            objectType = policy.getdSpaceObject().getType();
            main.setHead(T_main_head_edit.parameterize(policyID,Constants.typeText[objectType],objectID));
        }
        else
        {
            main.setHead(T_main_head_new.parameterize(Constants.typeText[objectType], objectID));
        }

        int resourceRelevance = 1 << objectType;


        // DIVISION: authorization-actions
        Division actions = main.addDivision("edit-policy-actions");
        List actionsList = actions.addList("actions","form");

        // name-description
        Text name = actionsList.addItem().addText("name");
        name.setLabel(T_label_name);
        name.setValue(rpName);

        TextArea description = actionsList.addItem().addTextArea("description");
        description.setLabel(T_label_description);
        description.setValue(rpDescription);


        // actions radio buttons
        actionsList.addLabel(T_label_action);
        Radio actionSelect = actionsList.addItem().addRadio("action_id");
        actionSelect.setLabel(T_label_action);
        for( int i = 0; i < Constants.actionText.length; i++ )
        {
            // only display if action i is relevant
            //  to resource type resourceRelevance                             
            if( (Constants.actionTypeRelevance[i] & resourceRelevance) > 0)
            {
                if (actionID == i)
                {
                    actionSelect.addOption(true, i, Constants.actionText[i]);
                }
                else
                {
                    actionSelect.addOption(i, Constants.actionText[i]);
                }
            }
        }
        if (errors.contains("action_id"))
        {
            actionSelect.addError(T_error_no_action);
        }



        // currently set group
        actionsList.addLabel(T_policy_currentGroup);
        Select groupSelect = actionsList.addItem().addSelect("group_id");
        for (Group group : groupService.findAll(context, null))
        {
            if (group == currentGroup)
            {
                groupSelect.addOption(true, group.getID().toString(), group.getName());
            }
            else
            {
                groupSelect.addOption(group.getID().toString(), group.getName());
            }
        }
        if (errors.contains("group_id"))
        {
            groupSelect.addError(T_error_no_group);
        }
        if (errors.contains("duplicatedPolicy")){
            groupSelect.addError(T_error_duplicated_policy);
        }


        if(dso instanceof Item || dso instanceof Bitstream){
            // start date
            Text startDate = actionsList.addItem().addText("start_date");
            startDate.setLabel(T_label_start_date);
            startDate.setValue(rpStartDate);
            startDate.setHelp(T_label_date_help);
            if (errors.contains("startDate"))
                startDate.addError(T_error_date_format);
            else if (errors.contains("startDateGreaterThenEndDate"))
                startDate.addError(T_error_start_date_greater_than_end_date);

            // end date
            Text endDate = actionsList.addItem().addText("end_date");
            endDate.setLabel(T_label_end_date);
            endDate.setValue(rpEndDate);
            endDate.setHelp(T_label_date_help);
            if (errors.contains("endDate"))
                endDate.addError(T_error_date_format);
        }




        // the search function
        actionsList.addLabel(T_label_search);
        org.dspace.app.xmlui.wing.element.Item searchItem = actionsList.addItem();
        Text searchText = searchItem.addText("query");
        if (!query.equals("-1"))
        {
            searchText.setValue(query);
        }
        searchItem.addButton("submit_search_groups").setValue(T_submit_search_groups);


        actionsList.addLabel();
        org.dspace.app.xmlui.wing.element.Item buttons = actionsList.addItem();
        buttons.addButton("submit_save").setValue(T_submit_save);
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);


        // Display the search results table
        if (!query.equals("-1")) {
            Division groupsList = main.addDivision("edit-policy-groupsList");
            groupsList.setHead(T_groups_head);
            this.addGroupSearch(groupsList, currentGroup, dso, query, page);
        }

        main.addHidden("administrative-continue").setValue(knot.getId());
    }

    /**
     * Search for groups to add to this group.
     */
    private void addGroupSearch(Division div, Group sourceGroup, DSpaceObject dso, String query, int page) throws WingException, SQLException
    {
        java.util.List<Group> groups = groupService.search(context, query, page*RESULTS_PER_PAGE, (page+1)*RESULTS_PER_PAGE);
        int totalResults = groupService.searchResultCount(context, query);
        ArrayList<ResourcePolicy> otherPolicies = (ArrayList<ResourcePolicy>) authorizeService.getPolicies(context, dso);


        if (totalResults > RESULTS_PER_PAGE) {
            int firstIndex = page*RESULTS_PER_PAGE+1;
            int lastIndex = page*RESULTS_PER_PAGE + groups.size();
            String baseURL = contextPath+"/admin/authorize?administrative-continue="+knot.getId();

            String nextURL = null, prevURL = null;
            if (page < ((totalResults - 1) / RESULTS_PER_PAGE))
            {
                nextURL = baseURL + "&page=" + (page + 1);
            }
            if (page > 0)
            {
                prevURL = baseURL + "&page=" + (page - 1);
            }

            div.setSimplePagination(totalResults,firstIndex,lastIndex,prevURL, nextURL);
        }


        Table table = div.addTable("policy-edit-search-group",groups.size() + 1, 1);

        Row header = table.addRow(Row.ROLE_HEADER);

        // Add the header row 
        header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_groups_column1);
        header.addCell().addContent(T_groups_column2);
        header.addCell().addContent(T_groups_column3);
        header.addCell().addContent(T_groups_column4);

        // The rows of search results
        for (Group group : groups)
        {
            String groupID = String.valueOf(group.getID());
            String name = group.getName();
            url = contextPath+"/admin/groups?administrative-continue="+knot.getId()+"&submit_edit_group&group_id="+groupID;

            Row row = table.addRow();
            row.addCell().addContent(groupID);
            row.addCell().addXref(url,name);

            // Iterate other other polices of our parent resource to see if any match the currently selected group
            StringBuilder otherAuthorizations = new StringBuilder();
            int groupsMatched = 0;
            for (ResourcePolicy otherPolicy : otherPolicies) {
                if (otherPolicy.getGroup() == group) {
                    otherAuthorizations.append(resourcePolicyService.getActionText(otherPolicy)).append(", ");
                    groupsMatched++;
                }
            }

            if (groupsMatched > 0) {
                row.addCell().addContent(otherAuthorizations.substring(0,otherAuthorizations.lastIndexOf(", ")));
            }
            else
            {
                row.addCell().addContent("-");
            }

            if (group != sourceGroup)
            {
                row.addCell().addButton("submit_group_id_" + groupID).setValue(T_set_group);
            }
            else
            {
                row.addCell().addContent(T_current_group);
            }

        }
        if (groups.size() <= 0) {
            table.addRow().addCell(1, 4).addContent(T_no_results);
        }
    }


}
