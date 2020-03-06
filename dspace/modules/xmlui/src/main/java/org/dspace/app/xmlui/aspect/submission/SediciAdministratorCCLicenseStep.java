package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpSession;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * basado en SediciCCLicenseStep
 */
public class SediciAdministratorCCLicenseStep extends AbstractSubmissionStep
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
        
        protected static final Message Commercial_question  = message("xmlui.Submission.submit.SediciCCLicenseStep.CommercialQuestion");
        protected static final Message Commercial_question_answer_no  = message("xmlui.Submission.submit.SediciCCLicenseStep.CommercialAnswerNo");
        protected static final Message Commercial_question_answer_yes  = message("xmlui.Submission.submit.SediciCCLicenseStep.CommercialAnswerYes");
        protected static final Message Derivatives_question  = message("xmlui.Submission.submit.SediciCCLicenseStep.DerivativesQuestion");
        protected static final Message Derivatives_question_answer_no  = message("xmlui.Submission.submit.SediciCCLicenseStep.DerivativesAnswerNo");
        protected static final Message Derivatives_question_answer_yes  = message("xmlui.Submission.submit.SediciCCLicenseStep.DerivativesAnswerYes");
        protected static final Message Derivatives_question_answer_sa  = message("xmlui.Submission.submit.SediciCCLicenseStep.DerivativesAnswerShareALike");
		private static final String PropertiesFilename = "sedici-dspace";
		private static TreeMap<String, String> Licencias=null;
	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public SediciAdministratorCCLicenseStep()
	{
	    this.requireSubmission = true;
	    this.requireStep = true;
	}
	
	public static TreeMap<String, String> GetLicenses(){
		if (Licencias==null || Licencias.size()<=1){
    			//cargo el map con las licencias
		        //Create a logical order comparator. This logical order is defined by the following constraints: 
		        // * (1)version number (i.e. code "2.5" is lesser than "4.0"), 
		        // * (2)Code restriction (i.e. "by" is less restrictive than "by-nc" and "by-nc-sa"; "by-nc" and "by-sa" are considered with equal restriction level...)
		        //Any license doesn't matching CC URI pattern, is considered bigger that those than match.
    		    Licencias = new TreeMap<String, String>(new Comparator<String>() {
    	            public int compare(String aLicense, String anotherLicense) {
    	                String patternCCUri = "^http://creativecommons.org/licenses/([[by|nc|sa|nd]-]+)/(\\d\\.\\d)/.*$";
    	                String patternCCCode = "^by-?(nc|sa|nd)?-?(sa|nd)?$";
    	                String[] licenseVersions = new String[2];
    	                String[] licenseCodes = new String[2];
    	                int[] licenseCodesWeight = new int[2];
    	                String[] licenses = new String[2];
    	                Pattern pUri, pCode;
    	                Matcher mUri, mCode;
    	                
    	                licenses[0] = aLicense; licenses[1] = anotherLicense;
    	                for (int i = 0; i < 2; i++) {
    	                    pUri = Pattern.compile(patternCCUri);
    	                    mUri = pUri.matcher(licenses[i]);
    	                    
    	                    if(mUri.find()) {
    	                        licenseVersions[i] = mUri.group(2);
    	                        licenseCodes[i] = mUri.group(1);
    	                        pCode = Pattern.compile(patternCCCode);
    	                        mCode = pCode.matcher(mUri.group(1));
    	                        mCode.find();
    	                        licenseCodesWeight[i] = 1 + (mCode.group(1) != null ? 1 : 0) + (mCode.group(2) != null ? 1 : 0);
    	                    }
    	                }
    	                
    	                //Check if any of the licenses being compared doesn't match the CC URI pattern, and return the correct compare if this is the case
    	                if(licenseVersions[0] == null || licenseVersions[1] == null) {
    	                    //Example, if compare "http://creativecommons.org/licenses/by/3.0/" with "http://creativecommons.org/publicdomain/mark/1.0/" (doesn't match CC pattern), 
    	                    //then comparison must returns "-1" indicating that the first license is lesser than the second
    	                    if(licenseVersions[0] != null) {
    	                        return -1;
    	                    } else if(licenseVersions[1] != null){
    	                        return 1;
    	                    } else {
    	                        //If both licenses doesn't match CC pattern, then compare syntactically
    	                        return aLicense.compareTo(anotherLicense);
    	                    }
    	                }

    	                //If both are CC licenses, then compare logically accordance with its components.
    	                //Compare version numbers (i.e. "2.5", "3.0", etc.). This is the most relevant criteria for order definition.
    	                if(licenseVersions[0].equals(licenseVersions[1])) {
    	                    //If versions are the same, then compare the CC Code of each license (i.e. "by", "by-sa", etc.).
    	                    if(licenseCodesWeight[0] == licenseCodesWeight[1]) {
    	                        return licenseCodes[0].compareTo(licenseCodes[1]);
    	                    } else {
    	                        if(licenseCodesWeight[0] < licenseCodesWeight[1])
    	                            return -1;
    	                        else
    	                            return 1;
    	                    }
    	                } else {
    	                    //If CC versions differs, then compare the licenses versions syntactically (i.e. check "3.0".compareTo("2.5")) to determine the precedence; 
    	                    //lesser version numbers must be at first place i.e. "2.5" < "3.0" < "4.0"
    	                    return licenseVersions[0].compareTo(licenseVersions[1]);
    	                }
    	            }
    	        });
			    String values=ConfigurationManager.getProperty(PropertiesFilename, "map.licenses");
		        String[] valores=values.split(",");
		        String[] valor;
		        for (String entrada : valores) {
					valor=entrada.split(" = ");
					Licencias.put(valor[0], valor[1]);
			}

		};
		return Licencias;
	}
	
	
	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{
	    // Build the url to and from creative commons
	    Item item = submission.getItem();
	    Collection collection=submission.getCollection();

	    String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
	    Request request = ObjectModelHelper.getRequest(objectModel);
	    
	    Division div = body.addInteractiveDivision("submit-cclicense", actionURL, Division.METHOD_POST, "primary submission");
	    div.setHead(T_submission_head);
	    addSubmissionProgressList(div);
	    HttpSession session = request.getSession();
        
	    // output the license selection options
	    List list = div.addList("licenseclasslist", List.TYPE_FORM);	    
	    list.addItem(T_info1);
	    list.setHead(T_head);	    
        //si es administrador de la colección se debe mostrar un select en vez de los radios
    	List edit = div.addList("selectlist1", List.TYPE_SIMPLE, "horizontalVanilla");
	    edit.addItem(message("xmlui.Submission.submit.SediciCCLicenseStep.administrador.pregunta"));
	    edit.addItem().addFigure(contextPath + "/themes/Reference/images/information.png", "javascript:void(0)", "El licenciador permite copiar, distribuir y comunicar públicamente la obra. A cambio, esta obra no puede ser utilizada con finalidades comerciales -- a menos que se obtenga el permiso del licenciador.", "information");
	    List subList = div.addList("sublist2", List.TYPE_SIMPLE, "horizontalVanilla");
	    Select select  = subList.addItem().addSelect("cc_license_chooser");
	    select.addOption("", message("xmlui.Submission.submit.SediciCCLicenseStep.administrador.sinLicencia"));
	    //Cargo la lista de licencias permitidas
        for (String key : GetLicenses().keySet()) {
        	select.addOption(key, message(GetLicenses().get(key)));
		}
        //En caso de que tenga una licencia la selecciono
	    String ccUri=ConfigurationManager.getProperty("cc.license.uri");
	    Metadatum[] carga=item.getMetadataByMetadataString(ccUri);
        if (carga.length>0){
	    	select.setOptionSelected(carga[0].value);
        } else {
        	//Si no tiene licencia, selecciono por default by-nc-sa
        	select.setOptionSelected("http://creativecommons.org/licenses/by-nc-sa/4.0/");
        }

		div.addSimpleHTMLFragment(true, "&#160;");

		Division statusDivision = div.addDivision("statusDivision");
		List statusList = statusDivision.addList("statusList", List.TYPE_FORM);

		if (session.getAttribute("isFieldRequired") != null && 	
		    session.getAttribute("isFieldRequired").equals("TRUE") && 
		    session.getAttribute("ccError") != null) 
		{
		    statusList.addItem().addHighlight("error").addContent(message((String)session.getAttribute("ccError")));
		    session.removeAttribute("ccError");
		    session.removeAttribute("isFieldRequired");
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

