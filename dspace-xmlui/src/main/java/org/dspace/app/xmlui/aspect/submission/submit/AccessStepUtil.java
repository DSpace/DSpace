/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.commons.lang.time.DateFormatUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.submit.step.AccessStep;
import org.dspace.submit.step.UploadWithEmbargoStep;

import java.sql.SQLException;


/**
 * This class represents a query which the discovery backend can use
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 *
 */
public class AccessStepUtil extends AbstractDSpaceTransformer {
    private Context context=null;

    protected static final Message T_name =message("xmlui.Submission.submit.AccessStep.name");
	protected static final Message T_name_help = message("xmlui.Submission.submit.AccessStep.name_help");
	protected static final Message T_reason = message("xmlui.Submission.submit.AccessStep.reason");
	protected static final Message T_reason_help = message("xmlui.Submission.submit.AccessStep.reason_help");
    protected static final Message T_radios_embargo = message("xmlui.Submission.submit.AccessStep.embargo_visible");
    protected static final Message T_groups = message("xmlui.Submission.submit.AccessStep.list_assigned_groups");
    protected static final Message T_item_will_be_visible = message("xmlui.Submission.submit.AccessStep.open_access");
    protected static final Message T_item_embargoed = message("xmlui.Submission.submit.AccessStep.embargo");
    protected static final Message T_error_date_format = message("xmlui.Submission.submit.AccessStep.error_format_date");
    protected static final Message T_error_missing_date = message("xmlui.Submission.submit.AccessStep.error_missing_date");
    protected static final Message T_error_duplicated_policy = message("xmlui.Submission.submit.AccessStep.error_duplicated_policy");

    // Policies Table
    protected static final Message T_no_policies = message("xmlui.Submission.submit.AccessStep.no_policies");
    protected static final Message T_head_policies_table = message("xmlui.Submission.submit.AccessStep.table_policies");
	protected static final Message T_policies_help = message("xmlui.Submission.submit.AccessStep.policies_help");
    protected static final Message T_column0 =message("xmlui.Submission.submit.AccessStep.column0");
    protected static final Message T_column1 =message("xmlui.Submission.submit.AccessStep.column1");
    protected static final Message T_column2 =message("xmlui.Submission.submit.AccessStep.column2");
    protected static final Message T_column3 =message("xmlui.Submission.submit.AccessStep.column3");
    protected static final Message T_column4 =message("xmlui.Submission.submit.AccessStep.column4");
    protected static final Message T_table_submit_edit =message("xmlui.Submission.submit.AccessStep.table_edit_button");
    protected static final Message T_table_submit_delete =message("xmlui.Submission.submit.AccessStep.table_delete_button");
	protected static final Message T_policy = message("xmlui.Submission.submit.AccessStep.review_policy_line");

    private static final Message T_label_date_help =
            message("xmlui.administrative.authorization.AccessStep.label_date_help");
    private static final Message T_label_date_displayonly_help =
            message("xmlui.administrative.authorization.AccessStep.label_date_displayonly_help");

    public static final int RADIO_OPEN_ACCESS_ITEM_VISIBLE=0;
    public static final int RADIO_OPEN_ACCESS_ITEM_EMBARGOED=1;

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();

    //public static final int CB_EMBARGOED=10;
    private String globalReason = null;

    private boolean isAdvancedFormEnabled=false;

    public AccessStepUtil(Context c){
        isAdvancedFormEnabled=DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);
        context=c;
    }

    public void addName(String name_, List form, int errorFlag) throws WingException {
        if(isAdvancedFormEnabled){
            Text name = form.addItem().addText("name");
	        name.setSize(0, 30);
            name.setLabel(T_name);
	        name.setHelp(T_name_help);

            if(name_!=null && errorFlag != org.dspace.submit.step.AccessStep.STATUS_COMPLETE)
                name.setValue(name_);
        }
    }

    public void addReason(String reason_, List form, int errorFlag) throws WingException {
        TextArea reason = form.addItem("reason", null).addTextArea("reason");
        reason.setLabel(T_reason);
	    reason.setHelp(T_reason_help);

        if(!isAdvancedFormEnabled){
            if(globalReason!=null)
                reason.setValue(globalReason);
        }
        else{
            if(reason_!=null && errorFlag != org.dspace.submit.step.AccessStep.STATUS_COMPLETE)
                reason.setValue(reason_);
        }
    }

    public void addListGroups(String groupID, List form, int errorFlag, Collection owningCollection) throws WingException, SQLException {

        if(isAdvancedFormEnabled){
            // currently set group
            form.addLabel(T_groups);
            Select groupSelect = form.addItem().addSelect("group_id");
            groupSelect.setMultiple(false);

            java.util.List<Group> loadedGroups = null;

            // retrieve groups
            String name = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("webui.submission.restrictstep.groups");
            if(name!=null){
                Group uiGroup = groupService.findByName(context, name);
                if(uiGroup!=null)
                    loadedGroups= uiGroup.getMemberGroups();
            }
            if(loadedGroups==null || loadedGroups.size() ==0){
                loadedGroups = groupService.findAll(context, null);
            }

            // if no group selected for default set anonymous
            if(groupID==null || groupID.equals("")) groupID= "0";
	        // when we're just loading the main step, also default to anonymous
	        if (errorFlag == AccessStep.STATUS_COMPLETE) {
		        groupID = "0";
	        }
            for (Group group : loadedGroups){
	            boolean selectGroup = group.getID().toString().equals(groupID);
	            groupSelect.addOption(selectGroup, group.getID().toString(), group.getName());
            }

            if (errorFlag == AccessStep.STATUS_DUPLICATED_POLICY || errorFlag == AccessStep.EDIT_POLICY_STATUS_DUPLICATED_POLICY
                    || errorFlag == UploadWithEmbargoStep.STATUS_EDIT_POLICIES_DUPLICATED_POLICY || errorFlag == UploadWithEmbargoStep.STATUS_EDIT_POLICY_DUPLICATED_POLICY){
                groupSelect.addError(T_error_duplicated_policy);
            }
        }

    }

    public void addAccessRadios(String selectedRadio, String date, List form, int errorFlag, DSpaceObject dso) throws WingException, SQLException {

        if(!isAdvancedFormEnabled){
            addEmbargoDateSimpleForm(dso, form, errorFlag);
        }
        else{

            org.dspace.app.xmlui.wing.element.Item radiosAndDate = form.addItem();
            Radio openAccessRadios = radiosAndDate.addRadio("open_access_radios");
            openAccessRadios.setLabel(T_radios_embargo);
            if(selectedRadio!=null && Integer.parseInt(selectedRadio)==RADIO_OPEN_ACCESS_ITEM_EMBARGOED
                    && errorFlag != org.dspace.submit.step.AccessStep.STATUS_COMPLETE){
                openAccessRadios.addOption(RADIO_OPEN_ACCESS_ITEM_VISIBLE, T_item_will_be_visible);
                openAccessRadios.addOption(true, RADIO_OPEN_ACCESS_ITEM_EMBARGOED, T_item_embargoed);
            }
            else{
                openAccessRadios.addOption(true, RADIO_OPEN_ACCESS_ITEM_VISIBLE, T_item_will_be_visible);
                openAccessRadios.addOption(RADIO_OPEN_ACCESS_ITEM_EMBARGOED, T_item_embargoed);
            }

            // Date
            Text startDate = radiosAndDate.addText("embargo_until_date");
            startDate.setLabel("");
            startDate.setHelp(T_label_date_help);
            if (errorFlag == org.dspace.submit.step.AccessStep.STATUS_ERROR_FORMAT_DATE){
                startDate.addError(T_error_date_format);
            }
            else if (errorFlag == org.dspace.submit.step.AccessStep.STATUS_ERROR_MISSING_DATE){
                startDate.addError(T_error_missing_date);
            }

            if(date!=null && errorFlag != org.dspace.submit.step.AccessStep.STATUS_COMPLETE){
                startDate.setValue(date);
            }
        }
    }

    public void addEmbargoDateDisplayOnly(final DSpaceObject dso, final List list) throws SQLException, WingException {
        final Text text = list.addItem().addText("embargo");
        text.setLabel(T_item_embargoed);
        text.setHelp(T_label_date_displayonly_help);
        populateEmbargoDetail(dso, text);
        text.setDisabled(true);
    }

    private void populateEmbargoDetail(final DSpaceObject dso, final Text text) throws SQLException, WingException {
        for (final ResourcePolicy readPolicy : authorizeService.getPoliciesActionFilter(context, dso, Constants.READ)) {
            if (Group.ANONYMOUS.equals(readPolicy.getGroup().getName()) && readPolicy.getStartDate() != null) {
                final String dateString = DateFormatUtils.format(readPolicy.getStartDate(), "yyyy-MM-dd");
                text.setValue(dateString);
                globalReason = readPolicy.getRpDescription();
            }
        }
    }

    public void addEmbargoDateSimpleForm(DSpaceObject dso, List form, int errorFlag) throws SQLException, WingException {
        Text startDate = form.addItem().addText("embargo_until_date");
        startDate.setLabel(T_item_embargoed);
        if (errorFlag == org.dspace.submit.step.AccessStep.STATUS_ERROR_FORMAT_DATE){
            startDate.addError(T_error_date_format);
        }
        else if (errorFlag == org.dspace.submit.step.AccessStep.STATUS_ERROR_MISSING_DATE){
            startDate.addError(T_error_missing_date);
        }

        if (dso != null) {
            populateEmbargoDetail(dso, startDate);
        }
        startDate.setHelp(T_label_date_help);
    }

    public void addTablePolicies(Division parent, DSpaceObject dso, Collection owningCollection) throws WingException, SQLException {
	    if (!isAdvancedFormEnabled) {
		    return;
	    }
	    Division div = parent.addDivision("access-existing-policies");
	    div.setHead(T_head_policies_table);
	    div.addPara(T_policies_help.parameterize(owningCollection));

	    java.util.List<ResourcePolicy> resourcePolicies = authorizeService.findPoliciesByDSOAndType(context, dso, ResourcePolicy.TYPE_CUSTOM);

	    if (resourcePolicies.isEmpty())
	    {
		    div.addPara(T_no_policies);
		    return;
	    }

	    int cols = resourcePolicies.size();
	    if(cols==0) cols=1;
	    Table policies = div.addTable("policies", 6, cols);
	    Row header = policies.addRow(Row.ROLE_HEADER);

	    header.addCellContent(T_column0); // name
	    header.addCellContent(T_column1); // action
	    header.addCellContent(T_column2); // group
	    header.addCellContent(T_column3); // start_date
	    header.addCellContent(T_column4); // end_date


	    for (ResourcePolicy rp : resourcePolicies){
	        int id = rp.getID();

	        String name = "";
	        if(rp.getRpName()!=null) name=rp.getRpName();

	        String action = resourcePolicyService.getActionText(rp);

	        // if it is the default policy for the Submitter don't show it.
	        if(dso instanceof org.dspace.content.Item){
	            org.dspace.content.Item item = (org.dspace.content.Item)dso;
	            if(rp.getEPerson()!=null){
	                if(item.getSubmitter().equals(rp.getEPerson()))
	                    continue;
	            }
	        }

	        String group = "";
	        if(rp.getGroup()!=null)
	            group = rp.getGroup().getName();

	        Row row = policies.addRow();

	        row.addCellContent(name);
	        row.addCellContent(action);
	        row.addCellContent(group);

	        // start
	        String startDate = "";
	        if(rp.getStartDate() != null){
	            startDate = DateFormatUtils.format(rp.getStartDate(), "yyyy-MM-dd");
	        }
	        row.addCellContent(startDate);

	        // endDate
	        String endDate = "";
	        if(rp.getEndDate() != null){
	            endDate = DateFormatUtils.format(rp.getEndDate(), "yyyy-MM-dd");
	        }
	        row.addCellContent(endDate);

	        Button edit = row.addCell().addButton("submit_edit_edit_policies_"+id);
	        edit.setValue(T_table_submit_edit);

	        Button delete = row.addCell().addButton("submit_delete_edit_policies_"+id);
	        delete.setValue(T_table_submit_delete);
	    }
    }

	public void addListPolicies(List parent, DSpaceObject dso, Collection owningCollection) throws WingException, SQLException {
		if (!isAdvancedFormEnabled) {
			return;
		}
		parent.addLabel(T_head_policies_table);

		java.util.List<ResourcePolicy> resourcePolicies = authorizeService.findPoliciesByDSOAndType(context, dso, ResourcePolicy.TYPE_CUSTOM);
		if (resourcePolicies.isEmpty()) {
			parent.addItem(T_no_policies);
			return;
		}


		for (ResourcePolicy rp : resourcePolicies){
			int id = rp.getID();

			String name = "";
			if(rp.getRpName()!=null) name=rp.getRpName();

			String action = resourcePolicyService.getActionText(rp);

			// if it is the default policy for the Submitter don't show it.
			if(dso instanceof org.dspace.content.Item){
				org.dspace.content.Item item = (org.dspace.content.Item)dso;
				if(rp.getEPerson() != null){
					if(item.getSubmitter().equals(rp.getEPerson()))
						continue;
				}
			}

			String group = "";
			if(rp.getGroup()!=null)
				group = rp.getGroup().getName();

			// start
			String startDate = "";
			if(rp.getStartDate() != null){
				startDate = DateFormatUtils.format(rp.getStartDate(), "yyyy-MM-dd");
			}

			// endDate
			String endDate = "";
			if(rp.getEndDate() != null){
				endDate = DateFormatUtils.format(rp.getEndDate(), "yyyy-MM-dd");
			}

			parent.addItem(T_policy.parameterize(name, action, group, startDate, endDate));
		}

	}
}
