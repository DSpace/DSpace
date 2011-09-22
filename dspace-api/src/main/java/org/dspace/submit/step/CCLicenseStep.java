/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.license.CreativeCommons;
import org.dspace.license.CCLookup;
import org.dspace.submit.AbstractProcessingStep;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

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

    private CCLookup cclookup = null;
    
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
                if (parameterName.equals("button_required")) {
                	isFieldRequired = "TRUE";
                	break;
                }
            }
            session.setAttribute("isFieldRequired", isFieldRequired);
            if (choiceButton.equals(SELECT_CHANGE)) {
	        Item item = subInfo.getSubmissionItem().getItem();
		if (CreativeCommons.hasLicense(item, "dc", "rights", "uri", Item.ANY) 
				&& !CreativeCommons.getRightsURI(item, "dc", "rights", "uri", Item.ANY).equals(""))
		{
        	    CreativeCommons.setItemMetadata(item, CreativeCommons.getRightsURI(item, "dc", "rights", "uri", Item.ANY), "dc", "rights", "uri", ConfigurationManager.getProperty("default.locale"));
                    item.addMetadata("dc", "rights", "uri", Item.ANY, CreativeCommons.getRightsURI(item, "dc", "rights", "uri", Item.ANY));
		    if (ConfigurationManager.getBooleanProperty("webui.submit.include.cc-name") == true) 
		    {
                       CreativeCommons.setItemMetadata(item, CreativeCommons.getRightsName(item, "dc", "rights", null, Item.ANY), "dc", "rights", null, ConfigurationManager.getProperty("default.locale"));
                       item.addMetadata("dc", "rights", null, Item.ANY, CreativeCommons.getRightsName(item, "dc", "rights", null, Item.ANY));
		    }
                    removeRequiredAttributes(session);
                    item.update();
                    context.commit();
                }
		return STATUS_COMPLETE;
            } else if (buttonPressed.startsWith(PROGRESS_BAR_PREFIX)
                    || buttonPressed.equals(PREVIOUS_BUTTON))
            {
                removeRequiredAttributes(session);
            }
            if (buttonPressed.equals(NEXT_BUTTON) || buttonPressed.equals(CANCEL_BUTTON)) {
                return processCC(context, request, response, subInfo);
            } else {  
                removeRequiredAttributes(session);
		session.removeAttribute("inProgress");
		return STATUS_COMPLETE;
            }
    }

    /**
     * Process the input from the CC license page
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
     * @return Status or error flag which will be processed by
     *         doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    protected int processCC(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException {
        String ccLicenseUrl = request.getParameter("cc_license_url");
        HttpSession session = request.getSession();
    	Map<String, String> map = new HashMap();
    	String licenseclass = (request.getParameter("licenseclass_chooser") != null) ? request.getParameter("licenseclass_chooser") : "";
    	String jurisdiction = (ConfigurationManager.getProperty("webui.submit.cc-jurisdiction") != null) ? ConfigurationManager.getProperty("webui.submit.cc-jurisdiction") : "";
    	if (licenseclass.equals("standard")) {
    		map.put("commercial", request.getParameter("commercial_chooser"));
    		map.put("derivatives", request.getParameter("derivatives_chooser"));
    	} else if (licenseclass.equals("recombo")) {
    		map.put("sampling", request.getParameter("sampling_chooser"));
    	}
    	map.put("jurisdiction", jurisdiction);
    	cclookup = new CCLookup();
    	cclookup.issue(licenseclass, map, ConfigurationManager.getProperty("default.locale"));
	Item item = subInfo.getSubmissionItem().getItem();
	if (licenseclass.equals("xmlui.Submission.submit.CCLicenseStep.no_license")) 
        {
	    // only remove any previous licenses
	    if (CreativeCommons.hasLicense(item, "dc", "rights", "uri", Item.ANY))
	    {
		CreativeCommons.removeLicenseMetadata(item, "dc", "rights", "uri", Item.ANY);
		item.update();
		context.commit();
		removeRequiredAttributes(session);
	    }
	    return STATUS_COMPLETE;
	 } else if (licenseclass.equals("xmlui.Submission.submit.CCLicenseStep.select_change"))
	 {
	    removeRequiredAttributes(session);
	    
	    return STATUS_COMPLETE;
	 }
	 else if (cclookup.isSuccess()) 
	 {
	     CreativeCommons.setItemMetadata(item, cclookup.getLicenseUrl(), "dc", "rights", "uri", ConfigurationManager.getProperty("default.locale"));
	     item.addMetadata("dc", "rights", "uri", Item.ANY, cclookup.getLicenseUrl());
             if (ConfigurationManager.getBooleanProperty("webui.submit.include.cc-name") == true) {
		CreativeCommons.setItemMetadata(item, cclookup.getLicenseName(), "dc", "rights", null, ConfigurationManager.getProperty("default.locale"));
                item.addMetadata("dc", "rights", null, Item.ANY, cclookup.getLicenseName());
	     }
	     item.update();
	     context.commit();
	     removeRequiredAttributes(session);
             session.removeAttribute("inProgress");
	  } 
	  else 
	  {
              request.getSession().setAttribute("ccError", cclookup.getErrorMessage());
	      if (CreativeCommons.hasLicense(item, "dc", "rights", "uri", Item.ANY))
	      {
	          CreativeCommons.removeLicenseMetadata(item, "dc", "rights", "uri", Item.ANY);
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
     * @param metaname -- license uri or license name
     * @param schema -- ex. dc
     * @param element -- ex. rights
     * @param qualifier -- ex. uri or null
     * @param lang -- ex. en_US or Item.ANY or ConfigurationManager.getProperty("default.locale")
     */
     private void setItemMetadata(Item item, String license, String schema, String element, String qualifier,
            String lang) throws java.sql.SQLException, java.io.IOException, org.dspace.authorize.AuthorizeException
     {
             DCValue[] dcvalues  = item.getMetadata(schema, element, qualifier, lang);
             ArrayList<String> arrayList = new ArrayList<String>();
             for (DCValue  dcvalue : dcvalues)
             {
		 boolean isUri = (qualifier != null) ? true : false;
                 if (addDCValue(dcvalue.value, isUri)) 
		 {
			arrayList.add(dcvalue.value);
	         }
              }
              String[] licenses = (String[])arrayList.toArray(new String[arrayList.size()]);
	      CreativeCommons.removeLicenseMetadata(item, schema, element, qualifier, Item.ANY);
              item.addMetadata(schema, element, qualifier, lang, licenses);
     }

     ArrayList<String> licenseNames = new ArrayList<String>();
     /**
      * @param metavalue - the hopeful dc value
      * @param isUri - true if uri
      * @param dcValue - the dc value
      * @return - a boolean to determine whether to the item's rights' metadata
      */
      private boolean addDCValue(String metavalue, boolean isUri) 
		throws java.io.IOException 
     {
	  if (isUri)
	  {
	       if (metavalue.indexOf("creativecommons") != -1)
               {
	           // first get the license type from the web service api to look at later for dc.rights.null
	           CCLookup cclookup = new CCLookup();
	           cclookup.issue(metavalue); // http://api.creativecommons.org/rest/1.5/details?license-uri=
	           licenseNames.add(cclookup.getLicenseName());
	           return false;
               } 
	   }
	   else
	   {
		// if the metavalue is in the original list then return false
	        if (licenseNames.contains(metavalue)) {
		    return false;
                }
	  }
	  return true;
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
