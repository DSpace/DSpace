/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;


import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.license.CCLicenseField;
import org.dspace.license.CCLookup;
import org.dspace.license.CCLicense;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.license.factory.LicenseServiceFactory;
import org.dspace.license.service.CreativeCommonsService;
import org.xml.sax.SAXException;

/**
 * This is an optional page of the item submission processes. The Creative 
 * Commons license may be added to an item in addition to the standard distribution 
 * license. This step will allow the user to go off to the creative commons website 
 * select a license and then when returned view what license was added to the item.
 * <P>
 * This class is called by org.dspace.app.xmlui.submission.step.LicenseStep
 * when the Creative Commons license is enabled
 * <P>
 * The form is divided into three major divisions: 1) A global div surrounds the 
 * whole page, 2) a specific interactive div displays the button that goes off to the 
 * creative commons website to select a license, and 3) a local division that displays 
 * the selected license and standard action bar.
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 * @author Wendy Bossons (updated for DSpace 1.8)
 */
public class CCLicenseStep extends AbstractSubmissionStep
{
	/** Language Strings **/
    protected static final Message T_head = 
        message("xmlui.Submission.submit.CCLicenseStep.head");
        protected static final Message T_select =
            message("xmlui.Submission.submit.CCLicenseStep.submit_choose_creative_commons");
    protected static final Message T_info1 = 
        message("xmlui.Submission.submit.CCLicenseStep.info1");
    protected static final Message T_submit_to_creative_commons = 
        message("xmlui.Submission.submit.CCLicenseStep.submit_to_creative_commons");
        protected static final Message T_submit_issue_creative_commons =
            message("xmlui.Submission.submit.CCLicenseStep.submit_issue_creative_commons");
    protected static final Message T_license = 
        message("xmlui.Submission.submit.CCLicenseStep.license");
        protected static final Message T_submit_remove = message("xmlui.Submission.submit.CCLicenseStep.submit_remove");
        protected static final Message T_no_license    = message("xmlui.Submission.submit.CCLicenseStep.no_license");
        protected static final Message T_select_change = message("xmlui.Submission.submit.CCLicenseStep.select_change");
        protected static final Message T_save_changes  = message("xmlui.Submission.submit.CCLicenseStep.save_changes");
        protected static final Message T_ccws_error  = message("xmlui.Submission.submit.CCLicenseStep.ccws_error");

    /** CC specific variables */
    private String ccLocale;

	protected CreativeCommonsService creativeCommonsService = LicenseServiceFactory.getInstance().getCreativeCommonsService();


	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public CCLicenseStep()
	{
	    this.requireSubmission = true;
	    this.requireStep = true;
        this.ccLocale = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("cc.license.locale");
        /** Default locale to 'en' */
        this.ccLocale = (this.ccLocale != null) ? this.ccLocale : "en";
	}
	
	
	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{
	    // Build the url to and from creative commons
	    Item item = submission.getItem();
	    Collection collection = submission.getCollection();
	    String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
	    Request request = ObjectModelHelper.getRequest(objectModel);
	    boolean https = request.isSecure();
	    String server = request.getServerName();
	    int port = request.getServerPort();
	    String exitURL = (https) ? "https://" : "http://";
	    exitURL += server;
	    if (! (port == 80 || port == 443))
	    	exitURL += ":"+port;
			
	    exitURL += actionURL + "?submission-continue="+knot.getId()+"&cc_license_url=[license_url]";
	    Division div = body.addInteractiveDivision("submit-cclicense", actionURL, Division.METHOD_POST, "primary submission");
	    div.setHead(T_submission_head);
	    addSubmissionProgressList(div);
	    CCLookup cclookup = new CCLookup();
	    HttpSession session = request.getSession();
        
	    // output the license selection options
	    String selectedLicense = request.getParameter("licenseclass_chooser");
	    List list = div.addList("licenseclasslist", List.TYPE_FORM);
	    list.addItem(T_info1);
	    list.setHead(T_head);
	    list.addItem().addHidden("button_required");
	    Select selectList = list.addItem().addSelect("licenseclass_chooser");
	    selectList.setLabel(T_license);
	    selectList.setEvtBehavior("submitOnChange");
	    Iterator<CCLicense> iterator = cclookup.getLicenses(ccLocale).iterator();
	    // build select List - first choice always 'choose a license', last always 'No license'
	    selectList.addOption(T_select_change.getKey(), T_select_change);
	    if(T_select_change.getKey().equals(selectedLicense)) {
	    	selectList.setOptionSelected(T_select_change.getKey());
	    }
	    while (iterator.hasNext()) {
	        CCLicense cclicense = iterator.next();
	        selectList.addOption(cclicense.getLicenseId(), cclicense.getLicenseName());
            if (selectedLicense != null && selectedLicense.equals(cclicense.getLicenseId()))
        	{
            	selectList.setOptionSelected(cclicense.getLicenseId());
        	}
	    }
	    selectList.addOption(T_no_license.getKey(), T_no_license);
	    if(T_no_license.getKey().equals(selectedLicense)) {
	    	selectList.setOptionSelected(T_no_license.getKey());
	    }
	    if (selectedLicense  !=  null) {
	    	// output the license fields chooser for the license class type
	    	if (cclookup.getLicenseFields(selectedLicense, ccLocale) == null ) {
	    		// do nothing
	    	} 
	    	else 
	    	{
		    Iterator outerIterator = cclookup.getLicenseFields(selectedLicense, ccLocale).iterator();
		    while (outerIterator.hasNext()) 
		    {
			CCLicenseField cclicensefield = (CCLicenseField)outerIterator.next();
			if (cclicensefield.getId().equals("jurisdiction"))  
			    continue;
			    List edit = div.addList("selectlist", List.TYPE_SIMPLE, "horizontalVanilla");
			    edit.addItem(cclicensefield.getLabel());
			    edit.addItem().addFigure(contextPath + "/themes/Reference/images/information.png", "javascript:void(0)", cclicensefield.getDescription(), "information");
			    List subList = div.addList("sublist", List.TYPE_SIMPLE, "horizontalVanilla");
			    Radio radio  = subList.addItem().addRadio(cclicensefield.getId() + "_chooser");
			    radio.setRequired();
			    Iterator fieldMapIterator = cclicensefield.getEnum().entrySet().iterator();
			    while (fieldMapIterator.hasNext()) 
			    {
			        Map.Entry pairs = (Map.Entry)fieldMapIterator.next();
				String key      = (String) pairs.getKey();
				String value 	= (String) pairs.getValue();
				radio.addOption(key, value);
			    }
				div.addSimpleHTMLFragment(true, "&#160;");
			}
		    }
        	}    
		Division statusDivision = div.addDivision("statusDivision");
		List statusList = statusDivision.addList("statusList", List.TYPE_FORM);
		String licenseUri = creativeCommonsService.getCCField("uri").ccItemValue(item);
		if (licenseUri != null)
		{
			statusList.addItem().addXref(licenseUri, licenseUri);
        }
		else
		{
			if (session.getAttribute("isFieldRequired") != null && 	
			    session.getAttribute("isFieldRequired").equals("TRUE") && 
			    session.getAttribute("ccError") != null) 
			{
			    statusList.addItem().addHighlight("error").addContent(T_ccws_error.parameterize((String)session.getAttribute("ccError")));
			    session.removeAttribute("ccError");
			    session.removeAttribute("isFieldRequired");
			} 
			else if (session.getAttribute("inProgress") != null && ((String)session.getAttribute("inProgress")).equals("TRUE")) 
			{
				statusList.addItem().addHighlight("italic").addContent(T_save_changes);
			}
		}
        addControlButtons(statusList);
        
	}

    /** 
     * Each submission step must define its own information to be reviewed
     * during the final Review/Verify Step in the submission process.
     * <P>
     * The information to review should be tacked onto the passed in 
     * List object.
     * <P>
     * NOTE: To remain consistent across all Steps, you should first
     * add a sub-List object (with this step's name as the heading),
     * by using a call to reviewList.addList().   This sublist is
     * the list you return from this method!
     * 
     * @param reviewList
     *      The List to which all reviewable information should be added
     * @return 
     *      The new sub-List object created by this step, which contains
     *      all the reviewable information.  If this step has nothing to
     *      review, then return null!   
     */
    public List addReviewSection(List reviewList) throws SAXException,
        WingException, UIException, SQLException, IOException,
        AuthorizeException
    {
        //nothing to review for CC License step
        return null;
    }
    
    
	/**
	 * Recycle
	 */
	public void recycle() 
	{
		super.recycle();
	}
}
