/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;

/**
 * Class representing configuration for a single step within an Item Submission
 * Process. In other words, this is a single step in the SubmissionConfig class.
 * This class represents the structure of a single 'step' node in the
 * item-submission.xml configuration file.
 *
 * Note: Implements Serializable as it will be saved to the current session during submission.
 * Please ensure that nothing is added to this class that isn't also serializable
 *
 * @author Tim Donohue
 * @version $Revision$
 * @see org.dspace.app.util.SubmissionConfigReader
 * @see org.dspace.app.util.SubmissionConfig
 */
public class SubmissionStepConfig implements Serializable {

    public static final String INPUT_FORM_STEP_NAME = "submission-form";
    public static final String UPLOAD_STEP_NAME = "upload";
    public static final String ACCESS_CONDITION_STEP_NAME = "accessCondition";

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
     * &lt;step-definitions&gt; section)
     */
    private String id = null;

    private boolean mandatory = true;

    /**
     * the heading for this step
     */
    private String heading = null;

    /**
     * the name of the java processing class for this step
     */
    private String processingClassName = null;

    /**
     * The name of the UI components for this step.
     **/
    private String type = null;


    /**
     * The scope restriction for this step (submission or workflow).
     **/
    private String scope = null;

    /**
     * visibility in the main scope (default=editable, eventually read-only)
     */
    private String visibility = null;

    /**
     * visibility outside the main scope (default=hidden, eventually read-only)
     */
    private String visibilityOutside = null;

    /**
     * The number of this step in the current SubmissionConfig
     */
    private int number = -1;

    /**
     * Class constructor for creating an empty SubmissionStepConfig object
     */
    public SubmissionStepConfig() {
    }

    /**
     * Class constructor for creating a SubmissionStepConfig object based on the
     * contents of a HashMap initialized by the SubmissionConfig object.
     *
     * @param stepMap the HashMap containing all required information about this
     *                step
     */
    public SubmissionStepConfig(Map<String, String> stepMap) {
        id = stepMap.get("id");
        String s = stepMap.get("mandatory");
        // only set if explicitly configured
        if (s != null) {
            mandatory = BooleanUtils.toBoolean(s);
        }
        heading = stepMap.get("heading");
        processingClassName = stepMap.get("processing-class");
        type = stepMap.get("type");
        scope = stepMap.get("scope");
        visibility = stepMap.get("scope.visibility");
        visibilityOutside = stepMap.get("scope.visibilityOutside");
    }

    /**
     * Get the ID for this step. An ID is only defined if the step exists in the
     * {@code <step-definitions>} section. This ID field is used to reference special
     * steps (like the required step with {@code id="collection"})
     *
     * @return the step ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get the heading for this step. This can either be a property from
     * Messages.properties, or the actual heading text. If this "heading"
     * contains a period(.) it is assumed to reference Messages.properties.
     *
     * @return the heading
     */
    public String getHeading() {
        return heading;
    }

    /**
     * Get the class which handles all processing for this step.
     * <p>
     * This class must extend the org.dspace.submit.AbstractProcessingStep class,
     * and provide processing for BOTH the JSP-UI and XML-UI
     *
     * @return the class's full class path (e.g.
     * "org.dspace.submit.step.MySampleStep")
     */
    public String getProcessingClassName() {
        return processingClassName;
    }

    /**
     * Retrieve the name of the component used by this step in the UI
     *
     * @return the name of the UI component to use for this step
     */
    public String getType() {
        return type;
    }

    /**
     * @return the scope restriction for this step
     */
    public String getScope() {
        return scope;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getVisibilityOutside() {
        return visibilityOutside;
    }

    /**
     * Get the number of this step in the current Submission process config.
     * Step numbers start with #0 (although step #0 is ALWAYS the special
     * "select collection" step)
     *
     * @return the number of this step in the current SubmissionConfig
     */
    public int getStepNumber() {
        return number;
    }

    /**
     * Sets the number of this step in the current Submission process config.
     * Step numbers start with #0 (although step #0 is ALWAYS the special
     * "select collection" step)
     *
     * @param stepNum the step number.
     */
    protected void setStepNumber(int stepNum) {
        this.number = stepNum;
    }

    /**
     * Whether or not this step is visible within the Progress Bar. A step is
     * only visible if it has been assigned a Heading, otherwise it's invisible
     *
     * @return if step is visible within the progress bar
     */
    public boolean isVisible() {
        return ((heading != null) && (heading.length() > 0));
    }

    public boolean isMandatory() {
        return mandatory;
    }
}
