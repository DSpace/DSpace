/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit;

import java.io.IOException;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract processing class for DSpace Submission Steps. This defines the base
 * methods which are required for any Step processing class.
 * <P>
 * This abstract class defines the base methods which are used by both the
 * Manakin XML UI and the JSP UI to perform submission step processing.
 * <P>
 * This includes the following methods:
 * <ul>
 * <li>doProcessing() method - called to perform any step processing</li>
 * <li>getErrorFields() method - called to determine the fields which errored
 * out during processing</li>
 * <li>getErrorMessage() method - called to determine any error message
 * returned after processing</li>
 * </ul>
 * <P>
 * If you are using the JSP UI (with the SubmissionController servlet) you
 * should extend the org.dspace.submit.SubmissionStep class, which defines
 * additional methods used to maintain the context of the submission within a
 * JSP environment!
 * 
 * @see org.dspace.app.webui.submit.JSPStepManager
 * @see org.dspace.app.webui.servlet.SubmissionController
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public abstract class AbstractProcessingStep
{
    /***************************************************************************
     * Constant - Name of the "<-Previous" button
     **************************************************************************/
    public static final String PREVIOUS_BUTTON = "submit_prev";

    /***************************************************************************
     * Constant - Name of the "Next->" button
     **************************************************************************/
    public static final String NEXT_BUTTON = "submit_next";

    /***************************************************************************
     * Constant - Name of the "Select" list
     **************************************************************************/
    public static final String SELECT_CHANGE = "submit_change";
	
	/***************************************************************************
     * Constant - Name of the "Cancel/Save" button
     **************************************************************************/
    public static final String CANCEL_BUTTON = "submit_cancel";

    /***************************************************************************
     * Constant - Prefix of all buttons in the Progress Bar
     **************************************************************************/
    public static final String PROGRESS_BAR_PREFIX = "submit_jump_";

    /***************************************************************************
     * Flag which specifies that the LAST PAGE of a step has been reached. This
     * flag is used when a Workflow Item is rejected (and returned to the
     * workspace) to specify that the LAST PAGE of the LAST STEP has already
     * been reached
     **************************************************************************/
    public static final int LAST_PAGE_REACHED = Integer.MAX_VALUE;

    /***************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     **************************************************************************/
    public static final int STATUS_COMPLETE = 0;

    /** Maps each status/error flag to a textual, human understandable message * */
    private Map<Integer, String> errorMessages = null;

    private static final String ERROR_FIELDS_ATTRIBUTE = "dspace.submit.error_fields";

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    
    /**
     * Do any processing of the information input by the user, and/or perform
     * step processing (if no user interaction required)
     * <P>
     * It is this method's job to save any data to the underlying database, as
     * necessary, and return error messages (if any) which can then be processed
     * by the doPostProcessing() method.
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
    public abstract int doProcessing(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
            SQLException, AuthorizeException;

    /**
     * Return a list of all UI fields which had errors that occurred during the
     * step processing. This list is for usage in generating the appropriate
     * error message(s) in the UI.
     * <P>
     * The list of fields which had errors should be set by the AbstractProcessingStep's
     * doProcessing() method, so that it can be accessed later by whatever UI is
     * generated.
     * 
     * @param request
     *            current servlet request object
     * @return List of error fields (as Strings)
     */
    public static final List<String> getErrorFields(HttpServletRequest request)
    {
        return (List<String>) request.getAttribute(ERROR_FIELDS_ATTRIBUTE);
    }
    
    /**
     * Sets the list of all UI fields which had errors that occurred during the
     * step processing. This list is for usage in generating the appropriate
     * error message(s) in the UI.
     * <P>
     * The list of fields which had errors should be set by the AbstractProcessingStep's
     * doProcessing() method, so that it can be accessed later by whatever UI is
     * generated.
     * 
     * @param request
     *            current servlet request object
     * @param errorFields
     *            List of all fields (as Strings) which had errors
     */
    private static final void setErrorFields(HttpServletRequest request, List<String> errorFields)
    {
        if(errorFields==null)
        {
            request.removeAttribute(ERROR_FIELDS_ATTRIBUTE);
        }
        else
        {
            request.setAttribute(ERROR_FIELDS_ATTRIBUTE, errorFields);
        }
    }

    /**
     * Add a single UI field to the list of all error fields (which can
     * later be retrieved using getErrorFields())
     * <P>
     * The list of fields which had errors should be set by the AbstractProcessingStep's
     * doProcessing() method, so that it can be accessed later by whatever UI is
     * generated.
     * 
     * @param fieldName
     *            the name of the field which had an error
     */
    protected static final void addErrorField(HttpServletRequest request, String fieldName)
    {
        //get current list
        List<String> errorFields = getErrorFields(request);
        
        if (errorFields == null)
        {
            errorFields = new ArrayList<String>();
        }

        //add this field
        errorFields.add(fieldName);
        
        //save updated list
        setErrorFields(request, errorFields);
    }

    /**
     * Clears the list of all fields that errored out during the previous step's
     * processing.
     * 
     * @param request
     *        current servlet request object
     * 
     */
    protected static final void clearErrorFields(HttpServletRequest request)
    {
        //get current list
        List<String> errorFields = getErrorFields(request);
        
        if (errorFields != null)
        {
            setErrorFields(request, null);
        }
    }

    /**
     * Return the text of an error message based on the passed in error flag.
     * These error messages are used for non-interactive steps (so that they can
     * log something more specific than just an error flag)
     * <P>
     * Since each step can define its own error messages and flags, this method
     * depends on all the error messages being initialized by using the
     * "addErrorMessage()" method within the constructor for the step class!
     * 
     * @param errorFlag
     *            The error flag defined in this step which represents an error
     *            message.
     * @return String which contains the text of the error message, or null if
     *         error message not found
     */
    public final String getErrorMessage(int errorFlag)
    {
        if (this.errorMessages == null || this.errorMessages.size() == 0)
        {
            return null;
        }
        else
        {
            return this.errorMessages.get(Integer.valueOf(errorFlag));
        }
    }

    /**
     * Add an error message to the internal map for this step.
     * <P>
     * This method associates a specific error message with an error flag
     * defined in this step.
     * <P>
     * This is extremely useful to define the error message which will be logged
     * for a non-interactive step.
     * 
     * @param errorFlag
     *            the status value indicating the type of error
     * @param errorMessage
     *            text of the message to be added
     */
    protected final void addErrorMessage(int errorFlag, String errorMessage)
    {
        if (this.errorMessages == null)
        {
            this.errorMessages = new HashMap<Integer, String>();
        }

        errorMessages.put(Integer.valueOf(errorFlag), errorMessage);
    }

    /**
     * Retrieves the number of pages that this "step" extends over. This method
     * is used by the SubmissionController to build the progress bar.
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
    public abstract int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException;

    /**
     * Find out which page a user is currently viewing
     * 
     * @param request
     *            HTTP request
     * 
     * @return current page
     */
    public static final int getCurrentPage(HttpServletRequest request)
    {
        int pageNum = -1;

        // try to retrieve cached page from request attribute
        Integer currentPage = (Integer) request.getAttribute("submission.page");

        if (currentPage == null)
        {
            // try and get it as a 'page' parameter
            String val = request.getParameter("page");

            try
            {
                pageNum =  Integer.parseInt(val.trim());
            }
            catch (Exception e)
            {
                // Problem with parameter
                pageNum = -1;
            }
            
            // if couldn't find page in request parameter
            if (pageNum < 0)
            {
                // default to page #1, since no other optionsc
                pageNum = 1;

                setCurrentPage(request, pageNum);
            }
            else
            {
                // save to request attribute
                setCurrentPage(request, pageNum);
            }
        }
        else
        {
            pageNum = currentPage.intValue();
        }

        return pageNum;
    }

    /**
     * Set which page a user is currently viewing
     * 
     * @param request
     *            HTTP request
     * @param pageNumber
     *            new current page
     */
    public static final void setCurrentPage(HttpServletRequest request,
            int pageNumber)
    {
        // set info to request
        request.setAttribute("submission.page", Integer.valueOf(pageNumber));
    }

}
