/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.RoleMembers;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This abstract class represents an api action
 * Each step in the xml workflow consists of a number of actions
 * this abstract action contains some utility methods and the methods
 * that each of these actions must implement including:
 * activating, execution, ... 
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class Action {

    private WorkflowActionConfig parent;
    private static String ERROR_FIELDS_ATTRIBUTE = "dspace.workflow.error_fields";


    public abstract void activate(Context c, XmlWorkflowItem wf) throws SQLException, IOException, AuthorizeException, WorkflowException;

    public abstract ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException, WorkflowException;

    public WorkflowActionConfig getParent() {
        return parent;
    }

    public void setParent(WorkflowActionConfig parent) {
        this.parent = parent;
    }

    public String getProvenanceStartId(){
        return "Step: " + getParent().getStep().getId() + " - action:" + getParent().getId();
    }

    public void alertUsersOnActivation(Context c, XmlWorkflowItem wfi, RoleMembers members) throws SQLException, IOException {

    }

    public abstract boolean isAuthorized(Context context, HttpServletRequest request, XmlWorkflowItem wfi) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException;


    /**
     * Sets th list of all UI fields which had errors that occurred during the
     * step processing. This list is for usage in generating the appropriate
     * error message(s) in the UI.
     *
     * @param request
     *            current servlet request object
     * @param errorFields
     *            List of all fields (as Strings) which had errors
     */
    private void setErrorFields(HttpServletRequest request, List errorFields)
    {
        if(errorFields==null)
            request.removeAttribute(ERROR_FIELDS_ATTRIBUTE);
        else
            request.setAttribute(ERROR_FIELDS_ATTRIBUTE, errorFields);
    }

    /**
     * Return a list of all UI fields which had errors that occurred during the
     * workflow processing. This list is for usage in generating the appropriate
     * error message(s) in the UI.
     *
     * @param request
     *            current servlet request object
     * @return List of error fields (as Strings)
     */
    public static List getErrorFields(HttpServletRequest request)
    {
        List result = new ArrayList();
        if(request.getAttribute(ERROR_FIELDS_ATTRIBUTE) != null)
            result = (List) request.getAttribute(ERROR_FIELDS_ATTRIBUTE);
        return result;
    }

    /**
     * Add a single UI field to the list of all error fields (which can
     * later be retrieved using getErrorFields())
     *
     * @param request
     *              current servlet request object
     * @param fieldName
     *            the name of the field which had an error
     */
    protected void addErrorField(HttpServletRequest request, String fieldName)
    {
        //get current list
        List errorFields = getErrorFields(request);

        if (errorFields == null)
        {
            errorFields = new ArrayList();
        }

        //add this field
        errorFields.add(fieldName);

        //save updated list
        setErrorFields(request, errorFields);
    }
}
