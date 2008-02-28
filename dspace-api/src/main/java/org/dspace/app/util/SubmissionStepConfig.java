/*
 * SubmissionStepConfig.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.util;

import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Class representing configuration for a single step within an Item Submission
 * Process. In other words, this is a single step in the SubmissionConfig class.
 * This class represents the structure of a single 'step' node in the
 * item-submission.xml configuration file.
 * 
 * @see org.dspace.app.util.SubmissionConfigReader
 * @see org.dspace.app.util.SubmissionConfig
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class SubmissionStepConfig
{
    /*
     * The identifier for the Select Collection step
     */
    public static String SELECT_COLLECTION_STEP = "collection";

    /*
     * The identifier for the Completion step
     */
    public static String COMPLETE_STEP = "complete";

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

    /** log4j logger */
    private static Logger log = Logger.getLogger(SubmissionStepConfig.class);

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
    public SubmissionStepConfig(Map stepMap)
    {
        id = (String) stepMap.get("id");
        heading = (String) stepMap.get("heading");
        processingClassName = (String) stepMap.get("processing-class");
        jspBindingClassName = (String) stepMap.get("jspui-binding");
        xmlBindingClassName = (String) stepMap.get("xmlui-binding");

        String wfEditString = (String) stepMap.get("workflow-editable");
        if (wfEditString != null && wfEditString.length() > 0)
        {
            workflowEditable = new Boolean(wfEditString).booleanValue();
        }
    }

    /**
     * Get the ID for this step. An ID is only defined if the step exists in the
     * <step-definitions> section. This ID field is used to reference special
     * steps (like the required step with id="collection")
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
