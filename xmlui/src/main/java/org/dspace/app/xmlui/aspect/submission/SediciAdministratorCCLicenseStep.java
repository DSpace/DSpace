package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder.Case;
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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.license.CreativeCommons;
import org.dspace.license.CCLicenseField;
import org.dspace.license.CCLookup;
import org.dspace.license.CCLicense;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

import ar.edu.unlp.sedici.aspect.redirect.RedirectAction;

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
		private static HashMap<String, String> Licencias=null;
	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public SediciAdministratorCCLicenseStep()
	{
	    this.requireSubmission = true;
	    this.requireStep = true;
	}
	
	public static HashMap<String, String> GetLicenses(){
		if (Licencias==null || Licencias.size()<=1){
				//cargo el map con las licencias
				Licencias=new HashMap<String, String>();
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
	    DCValue[] carga=item.getMetadata(ccUri);
        if (carga.length>0){
	    	select.setOptionSelected(carga[0].value);
        } else {
        	select.setOptionSelected("");
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

