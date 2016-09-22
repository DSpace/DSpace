/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.license.CCLookup;
import org.dspace.license.CreativeCommons;
import org.dspace.submit.AbstractProcessingStep;

/**
 * CCLicense step for DSpace Submission Process. 
 *
 * Processes the
 * user response to the license.
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized 
 * by both the JSP-UI and the Manakin XML-UI
 * <P>
 * 
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 * 
 * @author Tim Donohue
 * @author Wendy Bossons (based on earlier CC license step work, but updated
 * for DSpace 1.8 and CC web services api + curation)
 * @version $Revision$
 */
public class CCLicenseStep extends AbstractProcessingStep
{
    /***************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     * 
      * (Do NOT use status of 0, since it corresponds to STATUS_COMPLETE flag
     * defined in the JSPStepManager class)
     **************************************************************************/
    // user rejected the license
    public static final int STATUS_LICENSE_REJECTED = 1;

    /** log4j logger */
    private static Logger log = Logger.getLogger(CCLicenseStep.class);
    
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
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException,java.io.IOException
    {
	    HttpSession session = request.getSession();		
        session.setAttribute("inProgress", "TRUE");
        // check what submit button was pressed in User Interface
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);
	    String choiceButton = Util.getSubmitButton(request, SELECT_CHANGE);
        Enumeration e = request.getParameterNames();
        String isFieldRequired = "FALSE";
        while (e.hasMoreElements())
        {
            String parameterName = (String) e.nextElement();
            if (parameterName.equals("button_required"))
            {
            	isFieldRequired = "TRUE";
                break;
            }
        }
        session.setAttribute("isFieldRequired", isFieldRequired);
        if (choiceButton.equals(SELECT_CHANGE))
        {
	        Item item = subInfo.getSubmissionItem().getItem();
	        CreativeCommons.MdField uriField = CreativeCommons.getCCField("uri");
	        String licenseUri = uriField.ccItemValue(item);
	        if (licenseUri != null)
	        {
                removeRequiredAttributes(session);
            }
	        return STATUS_COMPLETE;
        }
        else if (buttonPressed.startsWith(PROGRESS_BAR_PREFIX) || buttonPressed.equals(PREVIOUS_BUTTON))
        {
            removeRequiredAttributes(session);
        }
        if (buttonPressed.equals(NEXT_BUTTON) || buttonPressed.equals(CANCEL_BUTTON) )
        {
            return processCC(context, request, response, subInfo);
        }
        else
        {  
            removeRequiredAttributes(session);
            session.removeAttribute("inProgress");
            return STATUS_COMPLETE;
        }
    }


    /**
     * Process the input from the CC license page using CC Web service
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     *
     * 
     * @return Status or error flag which will be processed by
     *         doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    protected int processCC(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException {
       
        HttpSession session = request.getSession();
    	Map<String, String> map = new HashMap<String, String>();
    	String licenseclass = (request.getParameter("licenseclass_chooser") != null) ? request.getParameter("licenseclass_chooser") : "";
    	String jurisdiction = (ConfigurationManager.getProperty("cc.license.jurisdiction") != null) ? ConfigurationManager.getProperty("cc.license.jurisdiction") : "";
    	if (licenseclass.equals("standard")) {
    		map.put("commercial", request.getParameter("commercial_chooser"));
    		map.put("derivatives", request.getParameter("derivatives_chooser"));
    	} else if (licenseclass.equals("recombo")) {
    		map.put("sampling", request.getParameter("sampling_chooser"));
    	}
    	map.put("jurisdiction", jurisdiction);
    	
    	CreativeCommons.MdField uriField = CreativeCommons.getCCField("uri");
    	CreativeCommons.MdField nameField = CreativeCommons.getCCField("name");
    	Item item = subInfo.getSubmissionItem().getItem();
    	if ("webui.Submission.submit.CCLicenseStep.no_license".equals(licenseclass) || "xmlui.Submission.submit.CCLicenseStep.no_license".equals(licenseclass))  
    	{
    		CreativeCommons.removeLicense(context, uriField, nameField, item);
    		
			item.update();
			context.commit();
			removeRequiredAttributes(session);
			
    		return STATUS_COMPLETE;
    	}
    	else if (StringUtils.isBlank(licenseclass) || "webui.Submission.submit.CCLicenseStep.select_change".equals(licenseclass) || "xmlui.Submission.submit.CCLicenseStep.select_change".equals(licenseclass))
    	{
    		removeRequiredAttributes(session);    
    		return STATUS_COMPLETE;
    	}
    	
    	CCLookup ccLookup = new CCLookup();
    	ccLookup.issue(licenseclass, map, ConfigurationManager.getProperty("cc.license.locale"));
    	if (ccLookup.isSuccess()) 
    	{
    		CreativeCommons.removeLicense(context, uriField, nameField, item);
    		
    		uriField.addItemValue(item, ccLookup.getLicenseUrl());
    		if (ConfigurationManager.getBooleanProperty("cc.submit.addbitstream")) {
    			CreativeCommons.setLicenseRDF(context, item, ccLookup.getRdf());
    		}	
    		if (ConfigurationManager.getBooleanProperty("cc.submit.setname")) {
    			nameField.addItemValue(item, ccLookup.getLicenseName());
    		}
    		
    		item.update();
    		context.commit();
    		removeRequiredAttributes(session);
    		session.removeAttribute("inProgress");
    	} 
    	else 
    	{
    		request.getSession().setAttribute("ccError", ccLookup.getErrorMessage());
    		String licenseUri = uriField.ccItemValue(item);
    		if (licenseUri != null)
    		{
    			uriField.removeItemValue(item, licenseUri);
    		}
    		return STATUS_LICENSE_REJECTED;
    	}
    	return STATUS_COMPLETE;
    }


	private void removeRequiredAttributes(HttpSession session) {
		session.removeAttribute("ccError");
		session.removeAttribute("isFieldRequired");
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
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
		return 1;
    }
   
}
