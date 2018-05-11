/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.Map;
import java.io.Serializable;

/**
 * Class representing configuration for a single step within an Item Submission
 * Process. In other words, this is a single step in the SubmissionConfig class.
 * This class represents the structure of a single 'step' node in the
 * item-submission.xml configuration file.
 *
 * Note: Implements Serializable as it will be saved to the current session during submission.
 * Please ensure that nothing is added to this class that isn't also serializable
 * 
 * @see org.dspace.app.util.SubmissionConfigReader
 * @see org.dspace.app.util.SubmissionConfig
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class SubmissionStepConfig implements Serializable
{
    /*
     * The identifier for the Select Collection step
     */
    public static final String SELECT_COLLECTION_STEP = "collection";

    /*
     * The identifier for the Completion step
     */
    public static final String COMPLETE_STEP = "complete";

    /**
     * the id for this step ('id' only exists if this step is defined in the
     * <step-definitions> section)
     */
    private String id = null;

    /** the heading for this step */
    private String heading = null;

    /** the name of the java processing class for this step */
    private String processingClassName = null;
   
    /** whether or not this step is editable during workflow (default=true) */
    private boolean workflowEditable = true;

    /** 
     * The full name of the JSP-UI binding class for this step. This field is
     * ONLY used by the JSP-UI.
     **/
    private String jspBindingClassName = null;

    /**
     * The full name of the Manakin XML-UI Transformer class which will generate
     * the necessary DRI for displaying this class in Manakin. This field is
     * ONLY used by the Manakin XML-UI.
     */
    private String xmlBindingClassName = null;

    /** The number of this step in the current SubmissionConfig */
    private int number = -1;

    /**
     * Class constructor for creating an empty SubmissionStepConfig object
     */
    public SubmissionStepConfig()
    {
    }

    /**
     * Class constructor for creating a SubmissionStepConfig object based on the
     * contents of a HashMap initialized by the SubmissionConfig object.
     * 
     * @param stepMap
     *            the HashMap containing all required information about this
     *            step
     */
    public SubmissionStepConfig(Map<String, String> stepMap)
    {
        id = stepMap.get("id");
        heading = stepMap.get("heading");
        processingClassName = stepMap.get("processing-class");
        jspBindingClassName = stepMap.get("jspui-binding");
        xmlBindingClassName = stepMap.get("xmlui-binding");

        String wfEditString = stepMap.get("workflow-editable");
        if (wfEditString != null && wfEditString.length() > 0)
        {
            workflowEditable = Boolean.parseBoolean(wfEditString);
        }
    }

    /**
     * Get the ID for this step. An ID is only defined if the step exists in the
     * {@code <step-definitions>} section. This ID field is used to reference special
     * steps (like the required step with {@code id="collection"})
     * 
     * @return the step ID
     */
    public String getId()
    {
        return id;
    }

    /**
     * Get the heading for this step. This can either be a property from
     * Messages.properties, or the actual heading text. If this "heading"
     * contains a period(.) it is assumed to reference Messages.properties.
     * 
     * @return the heading
     */
    public String getHeading()
    {
        return heading;
    }

    /**
     * Get the class which handles all processing for this step.
     * <p>
     * This class must extend the org.dspace.submit.AbstractProcessingStep class,
     * and provide processing for BOTH the JSP-UI and XML-UI
     * 
     * @return the class's full class path (e.g.
     *         "org.dspace.submit.step.MySampleStep")
     */
    public String getProcessingClassName()
    {
        return processingClassName;
    }

    /**
     * Retrieve the full class name of the Manakin Transformer which will
     * generate this step's DRI, for display in Manakin XML-UI.
     * <P>
     * This class must extend the
     * org.dspace.app.xmlui.aspect.submission.StepTransformer class.
     * <P>
     * This property is only used by the Manakin XML-UI, and therefore is not
     * relevant if you are using the JSP-UI.
     * 
     * @return the full java class name of the Transformer to use for this step
     */
    public String getXMLUIClassName()
    {
        return xmlBindingClassName;
    }
    
    /**
     * Retrieve the full class name of the JSP-UI "binding" class which will
     * initialize and call the necessary JSPs for display in the JSP-UI
     * <P>
     * This class must extend the
     * org.dspace.app.webui.submit.JSPStep class.
     * <P>
     * This property is only used by the JSP-UI, and therefore is not
     * relevant if you are using the XML-UI (aka. Manakin).
     * 
     * @return the full java class name of the JSPStep to use for this step
     */
    public String getJSPUIClassName()
    {
        return jspBindingClassName;
    }

    /**
     * Get the number of this step in the current Submission process config.
     * Step numbers start with #0 (although step #0 is ALWAYS the special
     * "select collection" step)
     * 
     * @return the number of this step in the current SubmissionConfig
     */
    public int getStepNumber()
    {
        return number;
    }

    /**
     * Sets the number of this step in the current Submission process config.
     * Step numbers start with #0 (although step #0 is ALWAYS the special
     * "select collection" step)
     * 
     * @param stepNum
     *            the step number.
     */
    protected void setStepNumber(int stepNum)
    {
        this.number = stepNum;
    }

    /**
     * Whether or not this step is editable during workflow processing. If
     * "true", then this step will appear in the "Edit Metadata" stage of the
     * workflow process.
     * 
     * @return if step is editable in a workflow process
     */
    public boolean isWorkflowEditable()
    {
        return workflowEditable;
    }

    /**
     * Whether or not this step is visible within the Progress Bar. A step is
     * only visible if it has been assigned a Heading, otherwise it's invisible
     * 
     * @return if step is visible within the progress bar
     */
    public boolean isVisible()
    {
        return ((heading != null) && (heading.length() > 0));
    }
}
