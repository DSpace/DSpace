package org.dspace.workflow.actions;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.Role;
import org.dspace.workflow.Step;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowItem;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 2-aug-2010
 * Time: 17:35:54
 * To change this template use File | Settings | File Templates.
 */
public abstract class Action {

    private WorkflowActionConfig parent;
    private static String ERROR_FIELDS_ATTRIBUTE = "dspace.workflow.error_fields";

    
    public abstract void activate(Context c, WorkflowItem wf) throws SQLException, IOException, AuthorizeException, WorkflowException;

    public abstract ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException, WorkflowException;

    public WorkflowActionConfig getParent() {
        return parent;
    }

    public void setParent(WorkflowActionConfig parent) {
        this.parent = parent;
    }

    public String getProvenanceStartId(){
        return "Step: " + getParent().getStep().getId() + " - action:" + getParent().getId();
    }

    public void alertUsersOnActivation(Context c, WorkflowItem wfi, EPerson[] members) throws SQLException, IOException {

    }

    public abstract boolean isAuthorized(Context context, HttpServletRequest request, WorkflowItem wfi) throws SQLException;


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
    private static void setErrorFields(HttpServletRequest request, List errorFields)
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
    protected static void addErrorField(HttpServletRequest request, String fieldName)
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
