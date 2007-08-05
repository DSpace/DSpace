/*
 * CCLicensePage.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.license.CreativeCommons;
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
 */
public class CCLicenseStep extends AbstractSubmissionStep
{
	/** Language Strings **/
    protected static final Message T_head = 
        message("xmlui.Submission.submit.CCLicenseStep.head");
    protected static final Message T_info1 = 
        message("xmlui.Submission.submit.CCLicenseStep.info1");
    protected static final Message T_submit_to_creative_commons = 
        message("xmlui.Submission.submit.CCLicenseStep.submit_to_creative_commons");
    protected static final Message T_license = 
        message("xmlui.Submission.submit.CCLicenseStep.license");
    protected static final Message T_submit_remove = 
        message("xmlui.Submission.submit.CCLicenseStep.submit_remove");
    protected static final Message T_no_license = 
        message("xmlui.Submission.submit.CCLicenseStep.no_license");
	
	/**
	 * The creative commons URL, where to send the user off to so that they can select a license.
	 */
	public final static String CREATIVE_COMMONS_URL = "http://creativecommons.org/license/";

	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public CCLicenseStep()
	{
		this.requireSubmission = true;
		this.requireStep = true;
	}
	
	
	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{
		// Build the url to and from creative commons
		Item item = submission.getItem();
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit";
		
		Request request = ObjectModelHelper.getRequest(objectModel);
		boolean https = request.isSecure();
		String server = request.getServerName();
		int port = request.getServerPort();
	
	    String exitURL = (https) ? "https://" : "http://";
	    exitURL += server;
	    if (! (port == 80 || port == 443))
	    	exitURL += ":"+port;
			
	    exitURL += actionURL + "?submission-continue="+knot.getId()+"&cc_license_url=[license_url]";
	
	    
	    
	    // Division 1:
	    //  Global division
		Division div = body.addDivision("submit-cclicense", "primary submission");
		div.setHead(T_submission_head);
        
        // Division 2:
		//Progress bar division
        Division progressDiv = div.addInteractiveDivision("submit-cclicense-progress", actionURL, Division.METHOD_POST);
        addSubmissionProgressList(progressDiv);
        //need 'submission-continue' in order to keep current state
        progressDiv.addHidden("submission-continue").setValue(knot.getId()); 
        
		// Division 3:
		//  Creative commons offsite division
	    Division offsiteDiv = div.addInteractiveDivision("submit-cclicense-offsite", CREATIVE_COMMONS_URL, Division.METHOD_POST);
        offsiteDiv.setHead(T_head);
	    offsiteDiv.addPara(T_info1);
		
	    offsiteDiv.addHidden("submission-continue").setValue(knot.getId()); 
	    offsiteDiv.addHidden("partner").setValue("dspace");
	    offsiteDiv.addHidden("exit_url").setValue(exitURL);
	    
	    Para ccPara = offsiteDiv.addPara("creative-commons-button","creative-commons-button");
	    ccPara.addButton("submit_to_creative_commons").setValue(T_submit_to_creative_commons);
	
	    // Division 4:
		//  Local onsite division
		Division onsiteDiv = div.addInteractiveDivision("submit-cclicense-offsite", actionURL, Division.METHOD_POST);
		List form = onsiteDiv.addList("submit-review", List.TYPE_FORM);
	
		form.addLabel(T_license);
		if (CreativeCommons.hasLicense(context, item))
		{
			String url = CreativeCommons.getLicenseURL(item);
			form.addItem().addXref(url,url);
			
			form.addItem().addButton("submit_no_cc").setValue(T_submit_remove);
			form.addItem().addHidden("cc_license_url").setValue(url);
        }
		else
		{
			form.addItem().addHighlight("italic").addContent(T_no_license);
		}
		
        
		// add standard control/paging buttons
        addControlButtons(form);
        
		onsiteDiv.addHidden("submission-continue").setValue(knot.getId()); 
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
