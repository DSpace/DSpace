/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package edu.tamu.dspace.proxylicense;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.LicenseUtils;
import org.dspace.core.LogManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

/**
 * This is the last step of the item submission processes. During this
 * step the user must agree to the collection's standard distribution
 * license. If the user can not agree to the license they they may either
 * save the submission untill a later time or remove the submission completely.
 * 
 * This step will include the full license text inside the page using the
 * HTML fragment method.
 * 
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 * @author Alexey Maslov
 */
public class XmluiProxyLicenseStep extends AbstractSubmissionStep
{
    private static final Logger log = Logger.getLogger(XmluiProxyLicenseStep.class);

    /** Language Strings **/
    protected static final Message T_head = 
	message("xmlui.Submission.submit.LicenseStep.head");
    protected static final Message T_info1 = 
	message("xmlui.Submission.submit.LicenseStep.info1");
    protected static final Message T_info2 = 
	message("xmlui.Submission.submit.LicenseStep.info2");
    protected static final Message T_info3 = 
	message("xmlui.Submission.submit.LicenseStep.info3");
    protected static final Message T_decision_label = 
	message("xmlui.Submission.submit.LicenseStep.decision_label");
    protected static final Message T_decision_checkbox = 
	message("xmlui.Submission.submit.LicenseStep.decision_checkbox");
    protected static final Message T_decision_error = 
	message("xmlui.Submission.submit.LicenseStep.decision_error");
    protected static final Message T_submit_remove = 
	message("xmlui.Submission.submit.LicenseStep.submit_remove");
    protected static final Message T_submit_complete = 
	message("xmlui.Submission.submit.LicenseStep.submit_complete");

   
    /**
     * Establish our required parameters, abstractStep will enforce these.
     */
    public XmluiProxyLicenseStep()
    {
	this.requireSubmission = true;
	this.requireStep = true;
    }


    public void addBody(Body body) throws SAXException, WingException,
    UIException, SQLException, IOException, AuthorizeException
    {
	Collection collection = submission.getCollection();
	String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";

	// Get the full text for the actual licenses
	String defaultLicenseText = LicenseUtils.getLicenseText(context.getCurrentLocale(), collection, submission.getItem(),
		submission.getSubmitter());
	String proxyLicenseText = loadLicense("proxy.license");
	String openLicenseText = loadLicense("open.license");;

	Division div = body.addInteractiveDivision("submit-license",actionURL, Division.METHOD_MULTIPART,"primary submission");
	div.setHead(T_submission_head);
	addSubmissionProgressList(div);

	Division inner = div.addDivision("submit-license-inner");
	inner.setHead("Granting a License");
	inner.addPara(T_info1);
	inner.addPara("Please select the license that best matches your situation, grant the license by selecting 'I Grant the License'; and then click 'Complete Submission'.");
	
	// Radio buttons that let you decide which license you want
	Radio licenseSelector = inner.addPara().addRadio("license-selector");
	licenseSelector.addOption("default", "I am the rights holder to this work");
	licenseSelector.addOption("proxy", "I am authorized by the rights holder to submit this work");
	licenseSelector.addOption("open", "I warrant this work is in the public domain");

	// Add the actual text of the licenses:
	Division defaultLicense = inner.addDivision("submit-license-standard-text","license-text");
	defaultLicense.addSimpleHTMLFragment(true, defaultLicenseText);
	
	Division proxyLicense = inner.addDivision("submit-license-proxy-text","license-text");
	proxyLicense.addSimpleHTMLFragment(true, proxyLicenseText);
	
	// Only add the upload capabilities for new item submissions
    	List upload = inner.addList("submit-proxy-document", List.TYPE_FORM);
    	org.dspace.app.xmlui.wing.element.File file = upload.addItem().addFile("file");
        file.setLabel("Proxy Submission License");
        file.setHelp("If you have a separate permission document from the " +
        		"copyright owner authorizing the release of this item, please attach it here.");
	
	Division openLicense = inner.addDivision("submit-license-open-text","license-text");
	openLicense.addSimpleHTMLFragment(true, openLicenseText);

	//inner.addPara(T_info3);

	List controls = inner.addList("submit-review", List.TYPE_FORM);

	CheckBox decision = controls.addItem().addCheckBox("decision");
	decision.setLabel(T_decision_label);
	decision.addOption("accept", "I accept the terms of this license.");
//	decision.addOption("acceptDefault", "I grant the license.");
//	decision.addOption("acceptProxy", "I am authorized to submit this work under terms of this agreement.");
//	decision.addOption("acceptOpen", "I warrant this item to be in Public Domain.");

	// If user did not check "I accept" checkbox 
	if(this.errorFlag==org.dspace.submit.step.LicenseStep.STATUS_LICENSE_REJECTED)
	{
	    log.info(LogManager.getHeader(context, "reject_license", submissionInfo.getSubmissionLogInfo()));

	    decision.addError(T_decision_error);
	}

	//add standard control/paging buttons
	addControlButtons(controls);
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
	//License step doesn't require reviewing
	return null;
    }
    
    
    
    /**
     * Helper method yanked from the Config Manager to get license file text
     * @param fileName (usually default.license, proxy.license, or open.license)
     * @return
     */
    protected static String loadLicense(String fileName) 
    {
	String license;
	
	// Load in default license
	
        File licenseFile = new File(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir") + File.separator
                + "config" + File.separator + fileName);

        FileInputStream  fir = null;
        InputStreamReader ir = null;
        BufferedReader br = null;
        try
        {
            
            fir = new FileInputStream(licenseFile);
            ir = new InputStreamReader(fir, "UTF-8");
            br = new BufferedReader(ir);
            String lineIn;
            license = "";

            while ((lineIn = br.readLine()) != null)
            {
                license = license + lineIn + '\n';
            }

            br.close();
            
        }
        catch (IOException e)
        {
            log.error("Can't load license: " + licenseFile.toString() , e);

            // FIXME: Maybe something more graceful here, but with the
            // configuration we can't do anything
            throw new IllegalStateException("Cannot load license: " + licenseFile.toString(),e);
        }
        finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                } 
                catch (IOException ioe)
                {                  
                }
            }

            if (ir != null)
            {
                try
                { 
                    ir.close();
                } 
                catch (IOException ioe)
                {             
                }
            }

            if (fir != null)
            {
                try
                {
                    fir.close();
                }
                catch (IOException ioe)
                {                
                }
            }
        }
	
	return license;
    }
    
    
    
    
    
}
