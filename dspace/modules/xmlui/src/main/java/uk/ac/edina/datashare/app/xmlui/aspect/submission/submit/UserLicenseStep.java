package uk.ac.edina.datashare.app.xmlui.aspect.submission.submit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.aspect.submission.submit.CCLicenseStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.submit.step.LicenseStep;
import org.xml.sax.SAXException;

import uk.ac.edina.datashare.utils.Consts;
import uk.ac.edina.datashare.utils.DSpaceUtils;
import uk.ac.edina.datashare.utils.MetaDataUtil;
import uk.ac.edina.datashare.utils.UserLicense;

/**
 * User license step. There are step distinct user license screens - No License,
 * Creative Commons and Open Data License.
 */
public class UserLicenseStep extends AbstractSubmissionStep
{
    private SourceResolver resolver = null;
    private String src = null;
    
    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.aspect.submission.AbstractStep#setup(org.apache.cocoon.environment.SourceResolver, java.util.Map, java.lang.String, org.apache.avalon.framework.parameters.Parameters)
     */
    @SuppressWarnings({ "rawtypes" })
    public void setup(
        SourceResolver resolver,
        Map objectModel,
        String src,
        Parameters parameters) 
        throws ProcessingException, SAXException, IOException
    {
        this.resolver = resolver;
        this.src = src;
        
        super.setup(resolver, objectModel, src, parameters);
    }
    
    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer#addBody(org.dspace.app.xmlui.wing.element.Body)
     */
    public void addBody(Body body) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
    {  
    	Collection collection = submission.getCollection();
    	Division div = body.addInteractiveDivision(
    			"submit-user-license",
    			contextPath + "/handle/"+ collection.getHandle() + "/submit/" + knot.getId() + ".continue",
    			Division.METHOD_POST,
    			"primary submission");
    	div.setHead(message("xmlui.Submission.general.submission.title"));
    	addSubmissionProgressList(div);
    	
    	List form = div.addList("submit-user-license", List.TYPE_FORM);
    	form.setHead(message("license.odc-attribution.header"));
    	
    	Select license = form.addItem().addSelect(Consts.USER_LICENSE_CONTROL);
    	license.setLabel(message("license.question.label"));
    	license.setHelp(message("license.question.help"));
    	
    	UserLicense currentLicense = DSpaceUtils.getUserLicenseType(submissionInfo);
          
    	// add license options
        license.addOption(
                currentLicense == UserLicense.CREATIVE_COMMONS_BY,
                UserLicense.CREATIVE_COMMONS_BY.ordinal(),
                "Creative Commons Attribution 4.0");
//    	license.addOption(
//    			currentLicense == UserLicense.ODC_ATTRIBUTION,
//    			UserLicense.ODC_ATTRIBUTION.ordinal(),
//    			"ODC Attribution");
//    	license.addOption(
//    			currentLicense == UserLicense.OPEN_DATA_COMMONS,
//    			UserLicense.OPEN_DATA_COMMONS.ordinal(),
//    			"Open Data Commons");
    	license.addOption(
    			currentLicense == UserLicense.NO_LICENSE,
    			UserLicense.NO_LICENSE.ordinal(),
    			message("license.nolicense.text"));

    	form.addLabel(message("license.nolicense.label"));
           
    	// rights text area
    	TextArea textArea = form.addItem().addTextArea("right-statement");
    	textArea.setHelp(message("license.nolicense.hint"));
           
    	// get current value from item
    	String currentValue = MetaDataUtil.getRights(submissionInfo);
           
    	if(currentValue != null)
    	{
    		textArea.setValue(currentValue);
    	}	
           
    	if(this.errorFlag == Consts.MANDITORY_FIELD)
    	{
    		textArea.addError(message("license.nolicense.error"));
    	}
           
    	addFooter(div, form);	
    }
    
    /**
     * Add generic header.
     * @param body Html body.
     * @param header Header string.
     * @return New top level division.
     * @throws WingException
     */
    private Division addHeader(Body body, Message header) throws WingException
    {
        // Division div = body.addDivision("user-license");
        Division div = body.addInteractiveDivision(
                "describe-license",
                contextPath + "/handle/" + submission.getCollection().getHandle() + "/submit/" + knot.getId() + ".continue",
                Division.METHOD_POST,
                "primary submission");
        
        div.setHead(header);
        
        // add progress buttons
        addSubmissionProgressList(div);
        
        return div;
    }
    
    /**
     * Add generic footer to page.
     * @param div The divisiobn to add the footer to.
     * @param controls The control to add the navigation buttons to.
     * @throws WingException
     */
    private void addFooter(Division div, List controls) throws WingException
    {
        // add previous / next buttons
        this.addControlButtons(controls);
    }
    
    /**
     * Add body for No License. 
     * @param body The body division.
     * @throws WingException
     */
    @SuppressWarnings("unused")
	private void doNoLicenseChosen(Body body) throws WingException
    {
        Division div = addHeader(body, message("license.nolicense.head"));
        
        // create control
        List options = div.addList("license-options", List.TYPE_FORM);
        options.setHead(message("license.nolicense.listhead"));
        options.addLabel(message("license.nolicense.label"));
        
        // rights text area
        TextArea textArea = options.addItem().addTextArea("right-statement");
        textArea.setHelp(message("license.nolicense.hint"));
        
        // get current value from item
        String currentValue = MetaDataUtil.getRights(submissionInfo);
        
        if(currentValue != null)
        {
            textArea.setValue(currentValue);
        }
        
        if(this.errorFlag == Consts.MANDITORY_FIELD)
        {
            textArea.addError(message("license.nolicense.error"));
        }
        
        addFooter(div, options);
    }
    
    /**
     * Creative Commons step. Pass call to DSpace CCLicenseStep.
     * @param body The page body.
     */
    @SuppressWarnings("unused")
	private void doCreativeCommons(Body body)
    {
        CCLicenseStep ccStep = new CCLicenseStep();
        
        try
        {
            ccStep.setup(resolver, objectModel, src, parameters);
            ccStep.addBody(body);
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Add ODC Attribution page.
     * @param body The page body.
     * @throws WingException
     */
    @SuppressWarnings("unused")
	private void doOdcAttribution(Body body) throws WingException
    {
        doOpenLicense(body, "odc-attribution");
    }
    
    /**
     * Add Open Data Commons page.
     * @param body The page body.
     * @throws WingException
     */
    @SuppressWarnings("unused")
	private void doOpenDataCommons(Body body) throws WingException
    {
        doOpenLicense(body, "open-data");
    }
    
    /**
     * Add generic license page.
     * @param body The page body.
     * @param license The license type string.
     * @throws WingException
     */
    private void doOpenLicense(Body body, String license) throws WingException
    {
        String tag = "license." + license;
        Division div = addHeader(body, message(tag + ".header1"));
        
        try
        {
            Division inner = div.addDivision("submit-license-inner");
            
            inner.setHead(message(tag + ".header2"));
            inner.addPara(message(tag + ".para1"));
            inner.addPara(message(tag + ".para2"));
            
            Para para = inner.addPara();
            para.addContent(message("license.link.para"));
            para.addXref(contextPath + "/docs/" + license + "_license.txt",
                    message(tag + ".link"));
            
            List controls = inner.addList("submit-review", List.TYPE_FORM);
            
            CheckBox decision =
                controls.addItem().addCheckBox(Consts.LICENSE_CHECKBOX);
            
            decision.setLabel(message("license.agreement.label"));
            decision.addOption(
                    Consts.LICENSE_ACCEPT,
                    message("xmlui.Submission.submit.LicenseStep.decision_checkbox"));

            // If user did not check "I accept" checkbox 
            if(this.errorFlag == LicenseStep.STATUS_LICENSE_REJECTED)
            {
                decision.addError(
                        message("xmlui.Submission.submit.LicenseStep.decision_error"));
            }
            
            addFooter(div, controls);
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Get the open commons license from file.
     * @return Open Data commons license as a string.
     */
    public String getODCLicenseText()
    {
        StringBuffer buffer = new StringBuffer();
        
        try
        { 
            String dir = ConfigurationManager.getProperty("dspace.dir") + File.separator + "config";
            String file = dir + File.separator + "open-data.license";
            BufferedReader br = new BufferedReader(new FileReader(file));
            
            String lineIn = null;
            
            while ((lineIn = br.readLine()) != null)
            {
                buffer.append(lineIn + '\n');
            }
            
            br.close();
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
        
        return buffer.toString();
    }
    
    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep#addReviewSection(org.dspace.app.xmlui.wing.element.List)
     */
    @Override
    public List addReviewSection(List reviewList) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException
    {
        return null;
    }
}
