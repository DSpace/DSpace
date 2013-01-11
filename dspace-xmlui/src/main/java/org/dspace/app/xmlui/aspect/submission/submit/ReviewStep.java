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
import java.util.Map;
import java.util.Set;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.aspect.submission.StepAndPage;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.submit.step.UploadStep;
import org.xml.sax.SAXException;

/**
 * This is a step of the item submission processes. This is where the user
 * reviews everything they have entered about the item up to this point.
 * <P>
 * This step is dynamic, since when using the Configurable Submission
 * it is unknown what steps are available and in what order.
 * <P>
 * This step builds a form with which consists of a separate section
 * for each step which implements the "addReviewSection()" method
 * of AbstractSubmissionStep class.
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 */
public class ReviewStep extends AbstractSubmissionStep
{

	/** Language Strings **/
    protected static final Message T_head = 
        message("xmlui.Submission.submit.ReviewStep.head");
    protected static final Message T_yes = 
        message("xmlui.Submission.submit.ReviewStep.yes");
    protected static final Message T_no = 
        message("xmlui.Submission.submit.ReviewStep.no");
    protected static final Message T_submit_jump = 
        message("xmlui.Submission.submit.ReviewStep.submit_jump");
    protected static final Message T_no_metadata = 
        message("xmlui.Submission.submit.ReviewStep.no_metadata");
    protected static final Message T_unknown = 
        message("xmlui.Submission.submit.ReviewStep.unknown");
    protected static final Message T_known = 
        message("xmlui.Submission.submit.ReviewStep.known");
    protected static final Message T_supported = 
        message("xmlui.Submission.submit.ReviewStep.supported");

    
     /* The SourceResolver used to setup this class */
    private SourceResolver resolver;
    
    /* The source string used to setup this class */
    private String src;
    
    /** log4j logger */
    private static Logger log = Logger.getLogger(UploadStep.class);

    
	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public ReviewStep()
	{
		this.requireSubmission = true;
		this.requireStep = true;
	}
   
    /**
     * Save these setup parameters, to use for loading up
     * the previous step's review information
     */
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) 
    throws ProcessingException, SAXException, IOException
    { 
        super.setup(resolver,objectModel,src,parameters);

        this.resolver = resolver;
        this.src = src;
    }
        
	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{
		// Get actionable URL
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";

        SubmissionConfig subConfig = submissionInfo.getSubmissionConfig();
        
        //Part A:
        // Build the main Review Form!
        Division div = body.addInteractiveDivision("submit-upload", actionURL, Division.METHOD_POST, "primary submission");
        div.setHead(T_submission_head);
        addSubmissionProgressList(div);
        
        List review = div.addList("submit-review", List.TYPE_FORM);
        review.setHead(T_head); 
        
        // Part B:
        // Add review section for each step
        
        //get a list of all pages in progress bar
        //(this is to ensure we are no looping through non-interactive steps)
        Set submissionPagesSet = submissionInfo.getProgressBarInfo().keySet();
        String[] submissionPages = (String[]) submissionPagesSet.toArray(new String[submissionPagesSet.size()]);
        
        //loop through each page in progress bar,
        //adding each as a separate section to the review form
        for(int i=0; i<submissionPages.length; i++)
        {
            StepAndPage currentStepAndPage = new StepAndPage(submissionPages[i]);
            
            //If the step we are looking at is this current
            // Review/Verify step, exit the for loop,
            // since we have completed all steps up to this one!
            if(currentStepAndPage.equals(this.stepAndPage))
            {
                break;
            }
            
            //load up step configuration
            SubmissionStepConfig stepConfig = subConfig.getStep(currentStepAndPage.getStep());
            
            //load the step's XML-UI Class
            AbstractStep stepUIClass = loadXMLUIClass(stepConfig.getXMLUIClassName());
            
            try
            {
                //initialize this class (with proper step parameter)
                parameters.setParameter("step", currentStepAndPage.toString());
                stepUIClass.setup(resolver, objectModel, src, parameters);
            }
            catch(Exception e)
            {
                throw new UIException("Unable to initialize AbstractStep identified by " 
                                        + stepConfig.getXMLUIClassName() + ":", e);
            }
            
            //If this stepUIClass is not a value AbstractSubmissionStep,
            //we will be unable to display its review information!
            if(stepUIClass instanceof AbstractSubmissionStep)
            {
                //add the Review section for this step, 
                //and return a reference to that newly created step section
                List stepSection = ((AbstractSubmissionStep) stepUIClass).addReviewSection(review);
                
                //as long as this step has something to review
                if(stepSection!=null)
                {    
                    //add a Jump To button for this section
                    addJumpButton(stepSection, T_submit_jump, currentStepAndPage);
                }
            }
            else
            {
                //Log a warning that this step cannot be reviewed!
                log.warn("The Step represented by " + stepConfig.getXMLUIClassName() + " is not a valid AbstractSubmissionStep, so it cannot be reviewed during the ReviewStep!");
            }   
        }
        
      
		// Part C:
        // add standard control/paging buttons
        addControlButtons(review); 
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
        //Review step cannot review itself :)
        return null;
    }
    
    /** 
     * Loads the specified XML-UI class
     * which will generate the review information
     * for a given step
     * 
     * @return AbstractStep which references
     *          the XML-UI Class
     */
    private AbstractStep loadXMLUIClass(String transformerClassName) 
        throws UIException
    {
        try
        {
            //retrieve an instance of the transformer class
            ClassLoader loader = this.getClass().getClassLoader();
            Class stepClass = loader
                    .loadClass(transformerClassName);
    
            // this XML-UI class *must* be a valid AbstractStep, 
            // or else we'll have problems here
            return (AbstractStep) stepClass
                        .newInstance();
        }
        catch(ClassNotFoundException cnfe)
        {
            //means that we couldn't find a class by the given name
            throw new UIException("Class Not Found: " + transformerClassName, cnfe);
        }
        catch(Exception e)
        {
            //means we couldn't instantiate the class as an AbstractStep
            throw new UIException("Unable to instantiate class " + transformerClassName + ". " +
                                          "Please make sure it extends org.dspace.app.xmlui.submission.AbstractSubmissionStep!", e);
        }
    }
    
}
