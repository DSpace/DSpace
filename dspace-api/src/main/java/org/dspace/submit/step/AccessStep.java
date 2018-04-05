/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.submit.AbstractProcessingStep;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;


/**
 * This class manages the access step during the submission
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 *
 */
public class AccessStep extends AbstractProcessingStep
{

    public static final int STATUS_ERROR_FORMAT_DATE = 1;
    public static final int STATUS_ERROR_MISSING_DATE = 2;
    public static final int STATUS_ERROR_SELECT_GROUP = 3;
    public static final int STATUS_DUPLICATED_POLICY = 4;
    public static final int EDIT_POLICY_STATUS_DUPLICATED_POLICY=5;


    // edit file information
    public static final int STATUS_EDIT_POLICY = 10;

    public static final String SUB_INFO_SELECTED_RP = "SUB_INFO_SELECTED_RP";

    /** log4j logger */
    private static Logger log = Logger.getLogger(AccessStep.class);

    // OPERATIONS
    public static final String FORM_EDIT_BUTTON_CANCEL = "submit_edit_cancel";
    public static final String FORM_EDIT_BUTTON_SAVE = "submit_save";
    public static final String FORM_ACCESS_BUTTON_ADD = "submit_add_policy";

    protected static GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected static AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected static ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();





    /**
     * Do any processing of the information input by the user, and/or perform
     * step processing (if no user interaction required)
     * <P>
     * It is this method's job to save any data to the underlying database, as
     * necessary, and return error messages (if any) which can then be processed
     * by the appropriate user interface (JSP-UI or XML-UI)
     * <P>
     * NOTE: If this step is a non-interactive step (i.e. requires no UI), then
     * it should perform *all* of its processing in this method!
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @return Status or error flag which will be processed by
     *         doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    @Override
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException{

        String buttonPressed = Util.getSubmitButton(request, CANCEL_BUTTON);

        // get reference to item
        Item item = subInfo.getSubmissionItem().getItem();

        Group group  = null;
        if(request.getParameter("group_id")!=null){
            try{
                group=groupService.find(context, UUID.fromString(request.getParameter("group_id")));
            }catch (NumberFormatException nfe){
                return STATUS_ERROR_SELECT_GROUP;
            }
        }

        String name = request.getParameter("name");
        String reason = request.getParameter("reason");


        // SELECTED OPERATION: go to EditPolicyForm
        if(wasEditPolicyPressed(context, buttonPressed, subInfo))
            return STATUS_EDIT_POLICY;

        // SELECTED OPERATION: Remove Policies
        if(wasRemovePolicyPressed(buttonPressed)){
            removePolicy(context, buttonPressed);
            context.dispatchEvents();
            return STATUS_COMPLETE;
        }

        // SELECTED OPERATION: Save or Cancel EditPolicy.
        if(comeFromEditPolicy(request)) {
            return saveOrCancelEditPolicy(context, request, subInfo, buttonPressed, item, name, group, reason);
        }


        // SELECTED OPERATION: ADD Policy
        if(wasAddPolicyPressed(buttonPressed)){

            int result=-1;
            if( (result = checkForm(request))!=0){
                return result;
            }

            // handle private checkbox
            item.setDiscoverable(true);
            if(request.getParameter("private_option")!=null){
                item.setDiscoverable(false);
            }

            Date dateStartDate = getEmbargoUntil(request);
            ResourcePolicy rp = null;
            if( (rp= authorizeService.createOrModifyPolicy(null, context, name, group, null, dateStartDate, org.dspace.core.Constants.READ, reason, item))==null){
                return STATUS_DUPLICATED_POLICY;
            }
            resourcePolicyService.update(context, rp);
            context.dispatchEvents();
            return STATUS_COMPLETE;
        }


        // if arrive here Next, Previous or Save has been pressed
        boolean isAdvancedFormEnabled= configurationService.getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);

        // if it is a simple form we should create the policy for Anonymous
        // if Anonymous does not have right on this collection, create policies for any other groups with
        // DEFAULT_ITEM_READ specified.
        if(!isAdvancedFormEnabled){
            int result = checkForm(request);
            if (result != 0)
            {
                return result;
            }
            authorizeService.generateAutomaticPolicies(context, getEmbargoUntilDate(request), reason, item, (Collection) handleService.resolveToObject(context, subInfo.getCollectionHandle()));
        }
//        else{
//            Date dateStartDate = getEmbargoUntil(request);
//            createOrModifyPolicy(null, context, name, groupID, null, dateStartDate, org.dspace.core.Constants.READ, reason, item);
//        }
        item.setDiscoverable(true);
        if(request.getParameter("private_option")!=null){
            item.setDiscoverable(false);
        }
        itemService.update(context, item);
        context.dispatchEvents();

        return STATUS_COMPLETE;
    }

    public static boolean wasEditPolicyPressed(Context context, String buttonPressed, SubmissionInfo subInfo) throws SQLException {
        if (buttonPressed.startsWith("submit_edit_edit_policies_") && !buttonPressed.equals(FORM_EDIT_BUTTON_CANCEL)){
            String idPolicy = buttonPressed.substring("submit_edit_edit_policies_".length());
            ResourcePolicy rp = resourcePolicyService.find(context, Integer.parseInt(idPolicy));
            subInfo.put(SUB_INFO_SELECTED_RP, rp);
            return true;
        }
        return false;
    }

    public boolean wasAddPolicyPressed(String buttonPressed) throws SQLException {
        return (buttonPressed.equalsIgnoreCase(FORM_ACCESS_BUTTON_ADD));
    }

    public static boolean wasRemovePolicyPressed(String buttonPressed) throws SQLException {
        return (buttonPressed.startsWith("submit_delete_edit_policies_"));
    }

    public static boolean comeFromEditPolicy(HttpServletRequest request) throws SQLException {
        return (request.getParameter("policy_id") != null);
    }

    public static int saveOrCancelEditPolicy(Context context, HttpServletRequest request, SubmissionInfo subInfo, String buttonPressed, DSpaceObject dso, String name, Group group, String reason) throws AuthorizeException, SQLException {
        if (buttonPressed.equals(FORM_EDIT_BUTTON_CANCEL)){
            return STATUS_COMPLETE;
        }
        else if (buttonPressed.equals(FORM_EDIT_BUTTON_SAVE)){
            String idPolicy = request.getParameter("policy_id");
            ResourcePolicy resourcePolicy = resourcePolicyService.find(context, Integer.parseInt(idPolicy));
            subInfo.put(SUB_INFO_SELECTED_RP, resourcePolicy);
            Date dateStartDate = getEmbargoUntil(request);
            if( (resourcePolicy= authorizeService.createOrModifyPolicy(resourcePolicy, context, name, group, null, dateStartDate, Constants.READ, reason, dso))==null){
                return EDIT_POLICY_STATUS_DUPLICATED_POLICY;
            }

            resourcePolicyService.update(context, resourcePolicy);
            context.dispatchEvents();
        }
        return STATUS_COMPLETE;
    }

    public static void removePolicy(Context context, String buttonPressed) throws SQLException, AuthorizeException {
        String idPolicy = buttonPressed.substring("submit_delete_edit_policies_".length());
        ResourcePolicy rp = resourcePolicyService.find(context, Integer.parseInt(idPolicy));
        resourcePolicyService.delete(context, rp);
    }

    public static int checkForm(HttpServletRequest request){

        String selectedRadio=null;
        String dateEmbargoUntil = request.getParameter("embargo_until_date");


        // RADIO_OPEN_ACCESS_ITEM_VISIBLE=0;
        // RADIO_OPEN_ACCESS_ITEM_EMBARGOED=1;
        if((selectedRadio=request.getParameter("open_access_radios"))!=null && Integer.parseInt(selectedRadio)==1
                && (dateEmbargoUntil==null || dateEmbargoUntil.equals(""))){
            return STATUS_ERROR_MISSING_DATE;
        }


        if(dateEmbargoUntil !=null && !dateEmbargoUntil.equals("")){
            Date startDate = getEmbargoUntilDate(request);
            if(startDate==null){
                return STATUS_ERROR_FORMAT_DATE;
            }
        }
        return 0;
    }







    public static Date getEmbargoUntil(HttpServletRequest request) {
        // RADIO_OPEN_ACCESS_ITEM_VISIBLE=0;
        // RADIO_OPEN_ACCESS_ITEM_EMBARGOED=1;
        String selectedRadio;
        Date dateStartDate=null;
        if((selectedRadio=request.getParameter("open_access_radios"))!=null && Integer.parseInt(selectedRadio)==1){
            Date startDate = getEmbargoUntilDate(request);
            if(startDate!=null) dateStartDate=startDate;
        }
        return dateStartDate;
    }

    private static Date getEmbargoUntilDate(HttpServletRequest request) {
        Date startDate = null;
        try {
            startDate = DateUtils.parseDate(request.getParameter("embargo_until_date"), new String[]{"yyyy-MM-dd", "yyyy-MM", "yyyy"});
        } catch (Exception e) {
            //Ignore start date is already null
        }
        return startDate;
    }

    /**
     * Retrieves the number of pages that this "step" extends over. This method
     * is used to build the progress bar.
     * <P>
     * This method may just return 1 for most steps (since most steps consist of
     * a single page). But, it should return a number greater than 1 for any
     * "step" which spans across a number of HTML pages. For example, the
     * configurable "Describe" step (configured using input-forms.xml) overrides
     * this method to return the number of pages that are defined by its
     * configuration file.
     * <P>
     * Steps which are non-interactive (i.e. they do not display an interface to
     * the user) should return a value of 1, so that they are only processed
     * once!
     * 
     * @param request
     *            The HTTP Request
     * @param subInfo
     *            The current submission information object
     * 
     * @return the number of pages in this step
     */
    @Override
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        return 1;

    }

}
