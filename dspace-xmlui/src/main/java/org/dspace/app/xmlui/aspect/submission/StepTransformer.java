/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map; 

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.SourceResolver;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.UserMeta;
import org.dspace.authorize.AuthorizeException;

import org.xml.sax.SAXException;

/**
 * This generic transformer is used to generate the DRI
 * for any Submission Step which extends the class
 * org.dspace.app.xmlui.submission.AbstractStep
 * <p>
 * This transformer just initializes the current step class
 * and calls the appropriate method(s) for the step.
 * 
 * @author Tim Donohue
 * @see AbstractStep
 */
public class StepTransformer extends AbstractDSpaceTransformer
{
	/**
     * The id of the currently active workspace or workflow, this contains 
     * the incomplete DSpace item 
     */
	protected String id;
	
	/**
	 * The current step and page's numeric value that it is at currently. This 
	 * number is dynamic between submissions and is a double where the integer
	 * value is the step #, and the decimal value is the page # 
	 * (e.g. 1.2 is page 2 of step 1)
	 */
	private double stepAndPage;
	
	/**
	 * Full name of the step's transformer class (which must extend
	 * org.dspace.app.xmlui.submission.AbstractStep).
	 */
	private String transformerClassName;
	
	/** 
     * The handle of the collection into which this DSpace
     * item is being submitted
     */
	protected String collectionHandle;
	
	/**
	 * An instance of the current step's transformer class (which must extend
	 * org.dspace.app.xmlui.submission.AbstractStep).  This class is
	 * used to generate the actual DRI for this step.
	 */
	private AbstractSubmissionStep step;
	
	
	/**
	 * Grab all the step's parameters from the sitemap. This includes 
	 * workspaceID, step, and a list of errored fields.
	 * 
	 * If the implementer set any required parameters then ensure that 
	 * they are all present.
	 */
	public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) 
	throws ProcessingException, SAXException, IOException
	{ 
		super.setup(resolver,objectModel,src,parameters);

		// retrieve id and transformer information
		// (This ID should always exist by this point, since the
		// selection of the collection should have already happened!)
		this.id = parameters.getParameter("id",null);
		this.transformerClassName = parameters.getParameter("transformer",null);
		
		// even though its not used in this class, this "step" parameter
		// is heavily used by the Transformers which extend the 
		// org.dspace.app.xmlui.submission.AbstractStep
		this.stepAndPage = Double.valueOf(parameters.getParameter("step","-1"));
		
		// retrieve collection handle if it's there
		this.collectionHandle = parameters.getParameter("handle",null);
		
		try
		{
	        // retrieve an instance of the transformer class
	        ClassLoader loader = this.getClass().getClassLoader();
	        Class stepClass = loader
	                .loadClass(this.transformerClassName);
	
	        // this XML-UI class *must* be a valid AbstractSubmissionStep, 
	        // or else we'll have problems here
	        step = (AbstractSubmissionStep) stepClass
	                    .newInstance();
		}
		catch(ClassNotFoundException cnfe)
		{
			// means that we couldn't find a class by the given name
			throw new ProcessingException("Class Not Found: " + this.transformerClassName, cnfe);
		}
	    catch(Exception e)
	    {
	    	// means we couldn't instantiate the class as an AbstractStep
	    	throw new ProcessingException("Unable to instantiate class " + this.transformerClassName + ". " +
	    								  "Please make sure it extends org.dspace.app.xmlui.submission.AbstractSubmissionStep!", e);
	    }
	
	    
	    // call the setup for this step
	    if(step!=null)
        {
            step.setup(resolver, objectModel, src, parameters);
        }
	    else
        {
            throw new ProcessingException("Step class is null!  We do not have a valid AbstractStep in " + this.transformerClassName + ". ");
        }
	}


	/** What to add at the end of the body */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException, ProcessingException
    {
        //call addBody for this step
    	step.addBody(body);
    }

    /** What to add to the options list */
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        //call addOptions for this step
    	step.addOptions(options);
    }

    /** What user metadata to add to the document */
    public void addUserMeta(UserMeta userMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
    	//call addUserMeta for this step
    	step.addUserMeta(userMeta);
    }

    /** What page metadata to add to the document */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
    	//call addPageMeta for this step
    	step.addPageMeta(pageMeta);
    }


	/**
	 * Recycle
	 */
	public void recycle() 
	{
		this.id = null;
		this.transformerClassName = null;
		this.collectionHandle = null;
		this.stepAndPage = -1;
		if(step!=null)
        {    
		    this.step.recycle();
		    this.step = null;
        }
		super.recycle();
	}
}
