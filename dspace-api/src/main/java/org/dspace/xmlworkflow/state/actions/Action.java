/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.RoleMembers;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * This abstract class represents a workflow action.
 * Each step in the workflow consists of a number of actions.
 * This abstract action contains some utility methods and the methods
 * that each of these actions must implement including:
 * activating, execution, ....
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class Action {

    private WorkflowActionConfig parent;
    private static final String ERROR_FIELDS_ATTRIBUTE = "dspace.workflow.error_fields";
    private List<String> advancedOptions = new ArrayList<>();
    private List<ActionAdvancedInfo> advancedInfo = new ArrayList<>();

    /**
     * Called when a workflow item becomes eligible for this Action.
     *
     * @param c current DSpace session.
     * @param wf the eligible item.
     * @throws SQLException passed through.
     * @throws IOException passed through.
     * @throws AuthorizeException passed through.
     * @throws WorkflowException passed through.
     */
    public abstract void activate(Context c, XmlWorkflowItem wf)
        throws SQLException, IOException, AuthorizeException, WorkflowException;

    /**
     * Called when the action is to be performed.
     *
     * @param c current DSpace session.
     * @param wfi the item on which the action is to be performed.
     * @param step the workflow step in which the action is performed.
     * @param request the current client request.
     * @return the result of performing the action.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     * @throws IOException passed through.
     * @throws WorkflowException passed through.
     */
    public abstract ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException, WorkflowException;

    /**
     * Returns a list of options that the user can select at this action which results in the next step in the workflow
     * @return  A list of options of this action, resulting in the next step of the workflow
     */
    public abstract List<String> getOptions();

    /**
     * Returns true if one of the options is a parameter of the request
     * @param request   Action request
     * @return  true if one of the options is a parameter of the request; false if none was found
     */
    protected boolean isOptionInParam(HttpServletRequest request) {
        for (String option: this.getOptions()) {
            if (request.getParameter(option) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the configuration of this Action.
     * @return details of this instance of an Action.
     */
    public WorkflowActionConfig getParent() {
        return parent;
    }

    /**
     * Configure this Action.
     * @param parent details of this instance of an Action.
     */
    public void setParent(WorkflowActionConfig parent) {
        this.parent = parent;
    }

    /**
     * Build provenance information for the action.
     * @return a String identifying the step and action.
     */
    public String getProvenanceStartId() {
        return "Step: " + getParent().getStep().getId() + " - action:" + getParent().getId();
    }

    /**
     * Notify action role members that an item requires action.
     *
     * @param c current DSpace session.
     * @param wfi the needy item.
     * @param members users who may fulfill the role.
     * @throws SQLException passed through.
     * @throws IOException passed through.
     */
    public void alertUsersOnActivation(Context c, XmlWorkflowItem wfi, RoleMembers members)
        throws SQLException, IOException {
    }

    /**
     * Is this client authorized to act on this item?
     *
     * @param context current DSpace session.
     * @param request current client request.
     * @param wfi the workflow item in question.
     * @return true if authorized.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     * @throws IOException passed through.
     * @throws WorkflowConfigurationException if the workflow is mis-configured.
     */
    public abstract boolean isAuthorized(Context context, HttpServletRequest request, XmlWorkflowItem wfi)
        throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException;

    /**
     * Sets the list of all UI fields which had errors that occurred during the
     * step processing. This list is for usage in generating the appropriate
     * error message(s) in the UI.
     *
     * @param request     current servlet request object
     * @param errorFields List of all fields (as Strings) which had errors
     */
    private void setErrorFields(HttpServletRequest request, List errorFields) {
        if (errorFields == null) {
            request.removeAttribute(ERROR_FIELDS_ATTRIBUTE);
        } else {
            request.setAttribute(ERROR_FIELDS_ATTRIBUTE, errorFields);
        }
    }

    /**
     * Return a list of all UI fields which had errors that occurred during the
     * workflow processing. This list is for usage in generating the appropriate
     * error message(s) in the UI.
     *
     * @param request current servlet request object
     * @return List of error fields (as Strings)
     */
    public static List getErrorFields(HttpServletRequest request) {
        List result = new ArrayList();
        if (request.getAttribute(ERROR_FIELDS_ATTRIBUTE) != null) {
            result = (List) request.getAttribute(ERROR_FIELDS_ATTRIBUTE);
        }
        return result;
    }

    /**
     * Add a single UI field to the list of all error fields (which can
     * later be retrieved using getErrorFields())
     *
     * @param request   current servlet request object
     * @param fieldName the name of the field which had an error
     */
    protected void addErrorField(HttpServletRequest request, String fieldName) {
        //get current list
        List errorFields = getErrorFields(request);

        if (errorFields == null) {
            errorFields = new ArrayList();
        }

        //add this field
        errorFields.add(fieldName);

        //save updated list
        setErrorFields(request, errorFields);
    }

    /**
     * Returns a list of advanced options that the user can select at this action
     * @return  A list of advanced options of this action, resulting in the next step of the workflow
     */
    protected List<String> getAdvancedOptions() {
        return advancedOptions;
    }

    /**
     * Returns true if this Action has advanced options, false if it doesn't
     * @return true if there are advanced options, false otherwise
     */
    protected boolean isAdvanced() {
        return !getAdvancedOptions().isEmpty();
    }

    /**
     * Returns a list of advanced info required by the advanced options
     * @return  A list of advanced info required by the advanced options
     */
    protected List<ActionAdvancedInfo> getAdvancedInfo() {
        return advancedInfo;
    }


    /**
     * Adds info in the metadata field dc.description.provenance about item being approved containing in which step
     * it was approved, which user approved it and the time
     *
     * @param c   DSpace contect
     * @param wfi Workflow item we're adding workflow accept provenance on
     */
    public void addApprovedProvenance(Context c, XmlWorkflowItem wfi) throws SQLException, AuthorizeException {
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();

        //Add the provenance for the accept
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName =
            XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService().getEPersonName(c.getCurrentUser());

        String provDescription = getProvenanceStartId() + " Approved for entry into archive by " + usersName + " on "
            + now + " (GMT) ";

        // Add to item as a DC field
        c.turnOffAuthorisationSystem();
        itemService.addMetadata(c, wfi.getItem(), MetadataSchemaEnum.DC.getName(), "description", "provenance", "en",
            provDescription);
        itemService.update(c, wfi.getItem());
        c.restoreAuthSystemState();
    }

}
