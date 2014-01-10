/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

import org.apache.log4j.Logger;

/**
 * Class representing a single Item Submission config definition, organized into
 * steps. This class represents the structure of a single 'submission-process'
 * node in the item-submission.xml configuration file.
 *
 * Note: Implements Serializable as it will be saved to the current session during submission.
 * Please ensure that nothing is added to this class that isn't also serializable
 * 
 * @see org.dspace.app.util.SubmissionConfigReader
 * @see org.dspace.app.util.SubmissionStepConfig
 * 
 * @author Tim Donohue, based on DCInputSet by Brian S. Hughes
 * @version $Revision$
 */

public class SubmissionConfig implements Serializable
{
    /** name of the item submission process */
    private String submissionName = null;

    /** the configuration classes for the steps in this submission process */
    private SubmissionStepConfig[] submissionSteps = null;

    /** whether or not this submission process is being used in a workflow * */
    private boolean isWorkflow = false;

    /** log4j logger */
    private static Logger log = Logger.getLogger(SubmissionConfig.class);

    /**
     * Constructs a new Submission Configuration object, based on the XML
     * configuration file (item-submission.xml)
     * 
     * @param submissionName
     *            the submission process name
     * @param steps
     *            the vector listing of step information to build
     *            SubmissionStepConfig objects for this submission process
     * @param isWorkflowProcess
     *            whether this submission process is being used in a workflow or
     *            not. If it is a workflow process this may limit the steps that
     *            are available for editing.
     */
    public SubmissionConfig(String submissionName, List<Map<String, String>> steps,
            boolean isWorkflowProcess)
    {
        this.submissionName = submissionName;
        this.isWorkflow = isWorkflowProcess;

        // initialize a vector of SubmissionStepConfig objects
        List<SubmissionStepConfig> stepConfigs = new ArrayList<SubmissionStepConfig>();

        // loop through our steps, and create SubmissionStepConfig objects
        for (int stepNum = 0; stepNum < steps.size(); stepNum++)
        {
            Map<String, String> stepInfo = steps.get(stepNum);
            SubmissionStepConfig step = new SubmissionStepConfig(stepInfo);

            // Only add this step to the process if either:
            // (a) this is not a workflow process OR
            // (b) this is a workflow process, and this step is editable in a
            // workflow
            if ((!this.isWorkflow)
                    || ((this.isWorkflow) && step.isWorkflowEditable()))
            {
                // set the number of the step (starts at 0) and add it
                step.setStepNumber(stepConfigs.size());
                stepConfigs.add(step);

                log.debug("Added step '" + step.getProcessingClassName()
                        + "' as step #" + step.getStepNumber()
                        + " of submission process " + submissionName);

            }
        }

        // get steps as an array of Strings
        submissionSteps = stepConfigs
                .toArray(new SubmissionStepConfig[stepConfigs.size()]);
    }

    /**
     * Return the name of the item submission process definition
     * 
     * @return the name of the submission process
     */
    public String getSubmissionName()
    {
        return submissionName;
    }

    /**
     * Return the number of steps in this submission process
     * 
     * @return number of steps
     */
    public int getNumberOfSteps()
    {
        return submissionSteps.length;
    }

    /**
     * Return whether or not this submission process is being used in a
     * workflow!
     * 
     * @return true, if it's a workflow process. false, otherwise.
     */
    public boolean isWorkflow()
    {
        return isWorkflow;
    }

    /**
     * Retrieve a particular Step configuration in this Item Submission Process
     * configuration. The first step is numbered "0" (although step #0 is the
     * implied "select collection" step).
     * <p>
     * If you want to retrieve the step after the "select collection" step, you
     * should retrieve step #1.
     * 
     * If the specified step isn't found, null is returned.
     * 
     * @param stepNum
     *            desired step to retrieve
     * 
     * @return the SubmissionStepConfig object for the step
     */

    public SubmissionStepConfig getStep(int stepNum)
    {
        if ((stepNum > submissionSteps.length - 1) || (stepNum < 0))
        {
            return null;
        }
        else
        {
            return submissionSteps[stepNum];
        }
    }

    /**
     * Returns whether or not there are more steps which follow the specified
     * "stepNum". For example, if you specify stepNum=4, then this method checks
     * to see if there is a Step #5. The first step is numbered "0".
     * 
     * @param stepNum
     *            the current step.
     * 
     * @return true, if a step at "stepNum+1" exists. false, otherwise.
     */

    public boolean hasMoreSteps(int stepNum)
    {
        return (getStep(stepNum + 1) != null);
    }
}
