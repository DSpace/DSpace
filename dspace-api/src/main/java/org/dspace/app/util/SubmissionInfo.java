/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.InProgressSubmission;

import org.dspace.submit.AbstractProcessingStep;
import org.dspace.workflow.WorkflowItem;

/**
 * Information about an item being editing with the submission UI
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class SubmissionInfo extends HashMap
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(SubmissionInfo.class);
    
    /** The item which is being submitted */
    private InProgressSubmission submissionItem = null;

    /**
    * The Submission process config, which holds all info about the submission
    * process that this item is going through (including all steps, etc)
    */
    private SubmissionConfig submissionConfig = null;
    
    /**
    * Handle of the collection where this item is being submitted
    */
    private String collectionHandle = null;
    
    /***************************************************************************
    * Holds all information used to build the Progress Bar in a key,value set.
    * Keys are the number of the step, followed by the number of the page
    * within the step (e.g. "2.1" = The first page of Step 2) (e.g. "5.2" = The
    * second page of Step 5) Values are the Headings to display for each step
    * (e.g. "Describe")
    **************************************************************************/
    private Map<String, String> progressBar = null;
    
    /** The element or element_qualifier to show more input boxes for */
    private String moreBoxesFor;

    /** The element or element_qualifier to scroll to initially using anchor */
    private String jumpToField;

    /** If non-empty, form-relative indices of missing fields */
    private List<String> missingFields;

    /** Specific bundle we're dealing with */
    private Bundle bundle;

    /** Specific bitstream we're dealing with */
    private Bitstream bitstream;
    
    /** Reader for submission process configuration file * */
    private static SubmissionConfigReader submissionConfigReader;
    
    /**
     * Default Constructor - PRIVATE
     * <p>
     * Create a SubmissionInfo object
     * using the load() method!
     * 
     */
    private SubmissionInfo()
    {
    }

    /**
     * Loads all known submission information based on the given in progress
     * submission and request object.
     * <P>
     * If subItem is null, then just loads the default submission information
     * for a new submission.
     * 
     * @param request
     *            The HTTP Servlet Request object          
     * @param subItem
     *            The in-progress submission we are loading information for
     * 
     * @return a SubmissionInfo object
     * 
     * @throws SubmissionConfigReaderException
     *             if an error occurs with the submission configuration retrieval
     */
    public static SubmissionInfo load(HttpServletRequest request, InProgressSubmission subItem) throws SubmissionConfigReaderException
    {
        boolean forceReload = false;
    	SubmissionInfo subInfo = new SubmissionInfo();
        
        // load SubmissionConfigReader only the first time
        // or if we're using a different UI now.
        if (submissionConfigReader == null)
        {
            submissionConfigReader = new SubmissionConfigReader();
            forceReload=true;
        }

        // save the item which is going through the submission process
        subInfo.setSubmissionItem(subItem);

        // Only if the submission item is created can we set its collection
        String collectionHandle = SubmissionConfigReader.DEFAULT_COLLECTION;
        if (subItem != null)
        {
            collectionHandle = subItem.getCollection().getHandle();
        }

        // save this collection handle to this submission info object
        subInfo.setCollectionHandle(collectionHandle);

        // load Submission Process config for this item's collection
        // (Note: this also loads the Progress Bar info, since it is
        // dependent on the Submission config)
        loadSubmissionConfig(request, subInfo, forceReload);

        return subInfo;
    }

    /**
     * Is this submission in the workflow process?
     * 
     * @return true if the current submission is in the workflow process
     */
    public boolean isInWorkflow()
    {
        return ((this.submissionItem != null) && this.submissionItem instanceof WorkflowItem);
    }

    /**
     * Return the current in progress submission
     * 
     * @return the InProgressSubmission object representing the current
     *         submission
     */
    public InProgressSubmission getSubmissionItem()
    {
        return this.submissionItem;
    }

    /**
     * Updates the current in progress submission item
     * 
     * @param subItem
     *            the new InProgressSubmission object
     */
    public void setSubmissionItem(InProgressSubmission subItem)
    {
        this.submissionItem = subItem;
    }

    /**
     * Return the current submission process config (which includes all steps
     * which need to be completed for the submission to be successful)
     * 
     * @return the SubmissionConfig object, which contains info on all the steps
     *         in the current submission process
     */
    public SubmissionConfig getSubmissionConfig()
    {
        return this.submissionConfig;
    }

    /**
     * Causes the SubmissionConfig to be completely reloaded from the XML
     * configuration file (item-submission.xml).
     * <P>
     * Note: This also reloads the progress bar info, since the progress bar
     * depends entirely on the submission process (and its steps).
     * 
     * @param request
     *            The HTTP Servlet Request object
     * 
	 * @throws SubmissionConfigReaderException
     *             if an error occurs with the submission configuration retrieval
     */
    public void reloadSubmissionConfig(HttpServletRequest request)
            throws SubmissionConfigReaderException
    {
        // Only if the submission item is created can we set its collection
        String collectionHandle = SubmissionConfigReader.DEFAULT_COLLECTION;
        if (this.submissionItem != null)
        {
            collectionHandle = submissionItem.getCollection().getHandle();
        }
        this.setCollectionHandle(collectionHandle);

        // force a reload of the submission process configuration
        loadSubmissionConfig(request, this, true);
    }

    /**
     * Returns a particular global step definition based on its ID.
     * <P>
     * Global step definitions are those defined in the {@code <step-definitions>}
     * section of the configuration file.
     * 
     * @param stepID
     *            step's identifier
     * 
     * @return the SubmissionStepConfig representing the step
     * 
     * @throws SubmissionConfigReaderException
     *             if no default submission process configuration defined
     */
    public SubmissionStepConfig getStepConfig(String stepID)
            throws SubmissionConfigReaderException
    {
        return submissionConfigReader.getStepConfig(stepID);
    }

    
    /**
     * Return text information suitable for logging.
     * <p>
     * This method is used by several of the Step classes
     * to log major events during the submission process (e.g. when
     * license agreement was accepted, when item was submitted, 
     * when it was available in DSpace, etc.)
     * 
     * @return the type and ID of the submission, bundle and/or bitstream for
     *         logging
     */
    public String getSubmissionLogInfo()
    {
        String info = "";

        if (isInWorkflow())
        {
            info = info + "workflow_id=" + getSubmissionItem().getID();
        }
        else
        {
            info = info + "workspace_item_id" + getSubmissionItem().getID();
        }

        if (getBundle() != null)
        {
            info = info + ",bundle_id=" + getBundle().getID();
        }

        if (getBitstream() != null)
        {
            info = info + ",bitstream_id=" + getBitstream().getID();
        }

        return info;
    }
    
    /**
     * Gets the handle of the collection to which this item is being submitted
     * 
     * @return the collection handle
     */
    public String getCollectionHandle()
    {
        return this.collectionHandle;
    }

    /**
     * Sets the handle of the collection to which this item is being submitted
     * 
     * @param handle
     *            the new collection handle
     */
    public void setCollectionHandle(String handle)
    {
        this.collectionHandle = handle;
    }

    /**
     * Return the information used to build the progress bar (this includes all
     * the steps in this submission, as well as the ordering and names of the
     * steps).
     * <p>
     * Returns a Hashmap, with the following specifics:
     * <p>
     * Keys are the number of the step, followed by the number of the page
     * within the step
     * <p>
     * (e.g. "2.1" = The first page of Step 2)
     * <p>
     * (e.g. "5.2" = The second page of Step 5)
     * <P>
     * Values are the Headings to display for each step (e.g. "Describe")
     * 
     * @return a Hashmap of Progress Bar information.
     */
    public Map<String, String> getProgressBarInfo()
    {
        return this.progressBar;
    }

    /**
     * Return the current bitstream we're working with (This is used during
     * upload processes, or user interfaces that are dealing with bitstreams)
     * 
     * @return the Bitstream object for the bitstream
     */
    public Bitstream getBitstream()
    {
        return this.bitstream;
    }

    /**
     * Sets the current bitstream we're working with (This is used during upload
     * processes, or user interfaces that are dealing with bitstreams)
     * 
     * @param bits
     *            the bitstream
     */
    public void setBitstream(Bitstream bits)
    {
        this.bitstream = bits;
    }

    /**
     * Return the current bundle we're working with (This is used during upload
     * processes, or user interfaces that are dealing with bundles/bitstreams)
     * 
     * @return the Bundle object for the bundle
     */
    public Bundle getBundle()
    {
        return this.bundle;
    }

    /**
     * Sets the current bundle we're working with (This is used during upload
     * processes, or user interfaces that are dealing with bundles/bitstreams)
     * 
     * @param bund
     *            the bundle
     */
    public void setBundle(Bundle bund)
    {
        this.bundle = bund;
    }

    /**
     * Return form related indices of the required fields which were not filled
     * out by the user.
     * 
     * @return a List of empty fields which are required
     */
    public List<String> getMissingFields()
    {
        return this.missingFields;
    }

    /**
     * Sets the form related indices of the required fields which were not
     * filled out by the user.
     * 
     * @param missing
     *            the List of empty fields which are required
     */
    public void setMissingFields(List<String> missing)
    {
        this.missingFields = missing;
    }

    /**
     * Return metadata field which user has requested more input boxes be
     * displayed (by pressing "Add More" on one of the "Describe" pages)
     * 
     * @return the String name of the field element
     */
    public String getMoreBoxesFor()
    {
        return this.moreBoxesFor;
    }

    /**
     * Sets the metadata field which user has requested more input boxes be
     * displayed (by pressing "Add More" on one of the "Describe" pages)
     * 
     * @param fieldname
     *            the name of the field element on the page
     */
    public void setMoreBoxesFor(String fieldname)
    {
        this.moreBoxesFor = fieldname;
    }

    /**
     * Return metadata field which JSP should "jump to" (i.e. set focus on) when
     * the JSP next loads. This is used during the Describe step.
     * 
     * @return the String name of the field element
     */
    public String getJumpToField()
    {
        return this.jumpToField;
    }

    /**
     * Sets metadata field which JSP should "jump to" (i.e. set focus on) when
     * the JSP next loads. This is used during the Describe step.
     * 
     * @param fieldname
     *            the name of the field on the page
     */
    public void setJumpToField(String fieldname)
    {
        this.jumpToField = fieldname;
    }

    /**
     * Load necessary information to build the Progress Bar for the Item
     * Submission Progress.
     * 
     * This information is returned in the form of a HashMap (which is then
     * stored as a part of the SubmissionInfo). The HashMap takes the following
     * form:
     * 
     * Keys - the number of the step, followed by the number of the page within
     * the step (e.g. "2.1" = The first page of Step 2) (e.g. "5.2" = The second
     * page of Step 5)
     * 
     * Values - the headings to display for each step (e.g. "Describe",
     * "Verify")
     * 
     * @param request
     *            The HTTP Servlet Request object
     * @param subInfo
     *            the SubmissionInfo object we are loading into
     * @param forceReload
     *            If true, this method reloads from scratch (and overwrites
     *            cached progress bar info)
     * 
     */
    private static void loadProgressBar(HttpServletRequest request,
            SubmissionInfo subInfo, boolean forceReload)
    {
        Map<String, String> progressBarInfo = null;

        log.debug("Loading Progress Bar Info");

        if (!forceReload)
        {
            // first, attempt to load from cache
            progressBarInfo = loadProgressBarFromCache(request
                    .getSession());
        }

        if (progressBarInfo != null && log.isDebugEnabled())
        {
            log.debug("Found Progress Bar Info in cache: "
                    + progressBarInfo.size()
                    + " pages to display in progress bar");
        }
        // if unable to load from cache, must load from scratch
        else
        {
            progressBarInfo = new LinkedHashMap<String, String>();

            // loop through all steps
            for (int i = 0; i < subInfo.submissionConfig.getNumberOfSteps(); i++)
            {
                // get the current step info
                SubmissionStepConfig currentStep = subInfo.submissionConfig
                        .getStep(i);
                String stepNumber = Integer.toString(currentStep
                        .getStepNumber());
                String stepHeading = currentStep.getHeading();

                // as long as this step is visible, include it in
                // the Progress Bar
                if (currentStep.isVisible())
                {
                    // default to just one page in this step
                    int numPages = 1;

                    try
                    {
                        // load the processing class for this step
                        ClassLoader loader = subInfo.getClass()
                                .getClassLoader();
                        Class<AbstractProcessingStep> stepClass = (Class<AbstractProcessingStep>)loader.loadClass(currentStep.getProcessingClassName());

                        // call the "getNumberOfPages()" method of the class
                        // to get it's number of pages
                        AbstractProcessingStep step = stepClass.newInstance();

                        // get number of pages from servlet
                        numPages = step.getNumberOfPages(request, subInfo);
                    }
                    catch (Exception e)
                    {
                        log.error(
                                "Error loading progress bar information from Step Class '"
                                        + currentStep.getProcessingClassName()
                                        + "' Error:", e);
                    }

                    // save each of the step's pages to the progress bar
                    for (int j = 1; j <= numPages; j++)
                    {
                        String pageNumber = Integer.toString(j);

                        // store ("stepNumber.pageNumber", Heading) for each
                        // page in the step
                        progressBarInfo.put(stepNumber + "." + pageNumber,
                                stepHeading);
                    }// end for each page
                }
            }// end for each step

            log.debug("Loaded Progress Bar Info from scratch: "
                    + progressBarInfo.size()
                    + " pages to display in progress bar");

            // cache this new progress bar
            saveProgressBarToCache(request.getSession(), progressBarInfo);
        }// end if null

        // save progressBarInfo to submission Info
        subInfo.progressBar = progressBarInfo;
    }

    /**
     * Saves all progress bar information into session cache. This saves us from
     * having to reload this same progress bar over and over again.
     * 
     * @param session
     *            The HTTP Session object
     * @param progressBarInfo
     *            The progress bar info to cache
     * 
     */
    private static void saveProgressBarToCache(HttpSession session,
            Map<String, String> progressBarInfo)
    {
        // cache progress bar info to Session
        session.setAttribute("submission.progressbar", progressBarInfo);
    }

    /**
     * Attempts to retrieve progress bar information (for a particular
     * collection) from session cache.
     * 
     * If the progress bar info cannot be found, returns null
     * 
     * @param session
     *            The HTTP Session object
     * 
     * @return progressBarInfo HashMap (if found), or null (if not)
     * 
     */
    private static Map<String, String> loadProgressBarFromCache(HttpSession session)
    {
        return (Map<String, String>) session.getAttribute("submission.progressbar");
    }

    /**
     * Loads SubmissionConfig object for the given submission info object. If a
     * SubmissionConfig object cannot be loaded, a Servlet Error is thrown.
     * <p>
     * This method just loads this SubmissionConfig object internally, so that
     * it is available via a call to "getSubmissionConfig()"
     * 
     * @param request
     *            The HTTP Servlet Request object
     * @param subInfo
     *            the SubmissionInfo object we are loading into
     * @param forceReload
     *            If true, this method reloads from scratch (and overwrites
     *            cached SubmissionConfig)
     * 
     */
    private static void loadSubmissionConfig(HttpServletRequest request,
            SubmissionInfo subInfo, boolean forceReload)
            throws SubmissionConfigReaderException
    {

        log.debug("Loading Submission Config information");

        if (!forceReload)
        {
            // first, try to load from cache
            subInfo.submissionConfig = loadSubmissionConfigFromCache(request
                    .getSession(), subInfo.getCollectionHandle(), subInfo
                    .isInWorkflow());
        }

        if (subInfo.submissionConfig == null || forceReload)
        {
            // reload the proper Submission process config
            // (by reading the XML config file)
            subInfo.submissionConfig = submissionConfigReader
                    .getSubmissionConfigByCollection(subInfo.getCollectionHandle());

            // cache this new submission process configuration
            saveSubmissionConfigToCache(request.getSession(),
                    subInfo.submissionConfig, subInfo.getCollectionHandle(),
                    subInfo.isInWorkflow());

            // also must force reload Progress Bar info,
            // since it's based on the Submission config
            loadProgressBar(request, subInfo, true);
        }
        else
        {
            log.debug("Found Submission Config in session cache!");

            // try and reload progress bar from cache
            loadProgressBar(request, subInfo, false);
        }
    }

    /**
     * Saves SubmissionConfig object into session cache. This saves us from
     * having to reload this object during every "Step".
     * 
     * @param session
     *            The HTTP Session object
     * @param subConfig
     *            The SubmissionConfig to cache
     * @param collectionHandle
     *            The Collection handle this SubmissionConfig corresponds to
     * @param isWorkflow
     *            Whether this SubmissionConfig corresponds to a workflow
     * 
     * 
     */
    private static void saveSubmissionConfigToCache(HttpSession session,
            SubmissionConfig subConfig, String collectionHandle,
            boolean isWorkflow)
    {
        // cache the submission process config
        // and the collection it corresponds to
        session.setAttribute("submission.config", subConfig);
        session.setAttribute("submission.config.collection", collectionHandle);
        session.setAttribute("submission.config.isWorkflow", Boolean.valueOf(
                isWorkflow));
    }

    /**
     * Loads SubmissionConfig object from session cache for the given
     * Collection. If a SubmissionConfig object cannot be found, null is
     * returned.
     * 
     * @param session
     *            The HTTP Session object
     * @param collectionHandle
     *            The Collection handle of the SubmissionConfig to load
     * @param isWorkflow
     *            whether or not we loading the Submission process for a
     *            workflow item
     * 
     * @return The cached SubmissionConfig for this collection
     */
    private static SubmissionConfig loadSubmissionConfigFromCache(
            HttpSession session, String collectionHandle, boolean isWorkflow)
    {
        // attempt to load submission process config
        // from cache for the current collection
        String cachedHandle = (String) session
                .getAttribute("submission.config.collection");

        Boolean cachedIsWorkflow = (Boolean) session
                .getAttribute("submission.config.isWorkflow");

        // only load from cache if the collection handle and
        // workflow item status both match!
        if (collectionHandle.equals(cachedHandle)
                && isWorkflow == cachedIsWorkflow.booleanValue())

        {
            return (SubmissionConfig) session.getAttribute("submission.config");
        }
        else
        {
            return null;
        }
    }
    
}

