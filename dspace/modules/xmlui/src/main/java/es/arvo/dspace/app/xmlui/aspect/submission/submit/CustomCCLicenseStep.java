/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package es.arvo.dspace.app.xmlui.aspect.submission.submit;


import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.aspect.submission.submit.CCLicenseStep;
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
import org.dspace.license.CreativeCommons;
import org.dspace.license.CCLicenseField;
import org.dspace.license.CCLookup;
import org.dspace.license.CCLicense;
import org.dspace.core.ConfigurationManager;
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
+ * the selected license and standard action bar.
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 * @author Wendy Bossons (updated for DSpace 1.8)
 */
public class CustomCCLicenseStep extends CCLicenseStep
{
        protected static final Message T_default_license    = message("xmlui.Submission.submit.CCLicenseStep.default_license");

        public void addBody(Body body) throws SAXException, WingException,
   	UIException, SQLException, IOException, AuthorizeException
    	{
    	    // Build the url to and from creative commons
    	    Item item = submission.getItem();
    	    Collection collection = submission.getCollection();
    	    String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
    	    if(StringUtils.isNotBlank(((org.apache.cocoon.environment.wrapper.RequestWrapper)this.objectModel.get("request")).getQueryString())){
    		actionURL+="?"+(((org.apache.cocoon.environment.wrapper.RequestWrapper)this.objectModel.get("request")).getQueryString());
    	    }
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
    	    Iterator<CCLicense> iterator = cclookup.getLicenses(ConfigurationManager.getProperty("default.locale")).iterator();
    	    // build select List - first choice always 'choose a license', last always 'No license'
    	    selectList.addOption(T_select_change.getKey(), T_select_change);
    	    if(ConfigurationManager.getBooleanProperty("cc.license.recomended.enabled")){
    	    	selectList.addOption("recomended", T_default_license);
    	    }
    	    while (iterator.hasNext()) {
    	        CCLicense cclicense = iterator.next();
    	        selectList.addOption(cclicense.getLicenseId(), cclicense.getLicenseName());
               if (selectedLicense != null && selectedLicense.equals(cclicense.getLicenseId()))
            	{
               	selectList.setOptionSelected(cclicense.getLicenseId());
            	}
    	    }
    	    if (selectedLicense != null && selectedLicense.equals("recomended"))
        	{
            	selectList.setOptionSelected("recomended");
        	}
    	    selectList.addOption(T_no_license.getKey(), T_no_license);
    	    if (selectedLicense  !=  null) {
    	    	// output the license fields chooser for the license class type
    	    	if (cclookup.getLicenseFields(selectedLicense,"es") == null ) {
    	    		// do nothing
    	    	} 
    	    	else 
    	    	{
    		    Iterator outerIterator = cclookup.getLicenseFields(selectedLicense,"es").iterator();
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
    		String licenseUri = CreativeCommons.getCCField("uri").ccItemValue(item);
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
}