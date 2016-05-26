/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

/**
 * Initial Submission servlet for DSpace. Handles the initial questions which
 * are asked to users to gather information regarding what metadata needs to be
 * gathered.
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized 
 * by both the JSP-UI and the Manakin XML-UI
 * <P>
 * 
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 * 
 * @author Tim Donohue
 * @version $Revision$
 */
public class InitialQuestionsStep extends AbstractProcessingStep
{
    /***************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     * 
     * (Do NOT use status of 0, since it corresponds to STATUS_COMPLETE flag
     * defined in the JSPStepManager class)
     **************************************************************************/
    // pruning of metadata needs to take place
    public static final int STATUS_VERIFY_PRUNE = 1;

    // pruning was cancelled by user
    public static final int STATUS_CANCEL_PRUNE = 2;

    // user attempted to upload a thesis, when theses are not accepted
    public static final int STATUS_THESIS_REJECTED = 3;

    /**
     * Global flags to determine if we need to prune anything
     */
    protected boolean willRemoveTitles = false;

    protected boolean willRemoveDate = false;

    protected boolean willRemoveFiles = false;

    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

    /**
     * Do any processing of the information input by the user, and/or perform
     * step processing (if no user interaction required)
     * <P>
     * It is this method's job to save any data to the underlying database, as
     * necessary, and return error messages (if any) which can then be processed
     * by the appropriate user interface (JSP-UI or XML-UI)
     * <P>
     * NOTE: If this step is a non-interactive step (i.e. requires no UI), then
     * it should perform *all* of its processing in this method!
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @return Status or error flag which will be processed by
     *         doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    @Override
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // Get the values from the initial questions form
        boolean multipleTitles = Util.getBoolParameter(request,
                "multiple_titles");
        boolean publishedBefore = Util.getBoolParameter(request,
                "published_before");
        boolean multipleFiles = Util.getBoolParameter(request,
                "multiple_files");
        boolean isThesis = configurationService.getBooleanProperty("webui.submit.blocktheses")
                && Util.getBoolParameter(request, "is_thesis");

        if (subInfo.isInWorkflow())
        {
            // Thesis question does not appear in workflow mode..
            isThesis = false;

            // Pretend "multiple files" is true in workflow mode
            // (There will always be the license file)
            multipleFiles = true;
        }

        // First and foremost - if it's a thesis, reject the submission
        if (isThesis)
        {
            WorkspaceItem wi = (WorkspaceItem) subInfo.getSubmissionItem();
            workspaceItemService.deleteAll(context, wi);
            subInfo.setSubmissionItem(null);

            // Remember that we've removed a thesis in the session
            request.getSession().setAttribute("removed_thesis",
                    Boolean.TRUE);

            return STATUS_THESIS_REJECTED; // since theses are disabled, throw
                                            // an error!
        }

        // Next, check if we are pruning some existing metadata
        Item item = subInfo.getSubmissionItem().getItem();
        if (request.getParameter("do_not_prune") != null)
        {
            return STATUS_CANCEL_PRUNE; // cancelled pruning!
        }
        else if (request.getParameter("prune") != null)
        {
            processVerifyPrune(context, request, response, subInfo,
                    multipleTitles, publishedBefore, multipleFiles);
        }
        else
        // otherwise, check if pruning is necessary
        {
            // Now check to see if the changes will remove any values
            // (i.e. multiple files, titles or an issue date.)

            if (subInfo.getSubmissionItem() != null)
            {
                // shouldn't need to check if submission is null, but just in case!
                if (!multipleTitles)
                {
                    List<MetadataValue> altTitles = itemService
                            .getMetadata(item, MetadataSchema.DC_SCHEMA, "title", "alternative", Item.ANY);

                    willRemoveTitles = altTitles.size() > 0;
                }

                if (!publishedBefore)
                {
                    List<MetadataValue> dateIssued = itemService
                            .getMetadata(item, MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY);
                    List<MetadataValue> citation = itemService
                            .getMetadata(item, MetadataSchema.DC_SCHEMA, "identifier", "citation", Item.ANY);
                    List<MetadataValue> publisher = itemService
                            .getMetadata(item, MetadataSchema.DC_SCHEMA, "publisher", null, Item.ANY);

                    willRemoveDate = (dateIssued.size() > 0)
                            || (citation.size() > 0) || (publisher.size() > 0);
                }

                if (!multipleFiles)
                {
                    // see if number of bitstreams in "ORIGINAL" bundle > 1
                    // FIXME: Assumes multiple bundles, clean up someday...
                    List<Bundle> bundles = itemService
                            .getBundles(item, "ORIGINAL");

                    if (bundles.size() > 0)
                    {
                        List<Bitstream> bitstreams = bundles.get(0).getBitstreams();

                        willRemoveFiles = bitstreams.size() > 1;
                    }
                }
            }

            // If anything is going to be removed from the item as a result
            // of changing the answer to one of the questions, we need
            // to inform the user and make sure that's OK, before saving!
            if (willRemoveTitles || willRemoveDate || willRemoveFiles)
            {
                //save what we will need to prune to request (for UI to process)
                request.setAttribute("will.remove.titles", Boolean.valueOf(willRemoveTitles));
                request.setAttribute("will.remove.date", Boolean.valueOf(willRemoveDate));
                request.setAttribute("will.remove.files", Boolean.valueOf(willRemoveFiles));

                return STATUS_VERIFY_PRUNE; // we will need to do pruning!
            }
        }

        // If step is complete, save the changes
        subInfo.getSubmissionItem().setMultipleTitles(multipleTitles);
        subInfo.getSubmissionItem().setPublishedBefore(publishedBefore);

        // "Multiple files" irrelevant in workflow mode
        if (!subInfo.isInWorkflow())
        {
            subInfo.getSubmissionItem().setMultipleFiles(multipleFiles);
        }

        // If this work has not been published before & no issued date is set,
        // then the assumption is that TODAY is the issued date.
        // (This logic is necessary since the date field is hidden on DescribeStep when publishedBefore==false)
        if(!publishedBefore)
        {
            List<MetadataValue> dateIssued = itemService
                            .getMetadata(item, MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY);
            if(dateIssued.size()==0)
            {
                //Set issued date to "today" (NOTE: InstallItem will determine the actual date for us)
                itemService.addMetadata(context, item, MetadataSchema.DC_SCHEMA, "date", "issued", null, "today");
            }
        }

        // commit all changes to DB
        ContentServiceFactory.getInstance().getInProgressSubmissionService(subInfo.getSubmissionItem()).update(context, subInfo.getSubmissionItem());
        context.dispatchEvents();

        return STATUS_COMPLETE; // no errors!
    }

    /**
     * Retrieves the number of pages that this "step" extends over. This method
     * is used to build the progress bar.
     * <P>
     * This method may just return 1 for most steps (since most steps consist of
     * a single page). But, it should return a number greater than 1 for any
     * "step" which spans across a number of HTML pages. For example, the
     * configurable "Describe" step (configured using input-forms.xml) overrides
     * this method to return the number of pages that are defined by its
     * configuration file.
     * <P>
     * Steps which are non-interactive (i.e. they do not display an interface to
     * the user) should return a value of 1, so that they are only processed
     * once!
     * 
     * @param request
     *            The HTTP Request
     * @param subInfo
     *            The current submission information object
     * 
     * @return the number of pages in this step
     */
    @Override
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        // always just one page of initial questions
        return 1;
    }

    /**
     * Process input from "verify prune" page
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @param multipleTitles
     *            if there is multiple titles
     * @param publishedBefore
     *            if published before
     * @param multipleFiles
     *            if there will be multiple files
     */
    protected void processVerifyPrune(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo, boolean multipleTitles,
            boolean publishedBefore, boolean multipleFiles)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // get the item to prune
        Item item = subInfo.getSubmissionItem().getItem();

        if (!multipleTitles && subInfo.getSubmissionItem().hasMultipleTitles())
        {
            itemService.clearMetadata(context, item, MetadataSchema.DC_SCHEMA, "title", "alternative", Item.ANY);
        }

        if (!publishedBefore && subInfo.getSubmissionItem().isPublishedBefore())
        {
            itemService.clearMetadata(context, item, MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY);
            itemService.clearMetadata(context, item, MetadataSchema.DC_SCHEMA, "identifier", "citation", Item.ANY);
            itemService.clearMetadata(context, item, MetadataSchema.DC_SCHEMA, "publisher", null, Item.ANY);
        }

        if (!multipleFiles && subInfo.getSubmissionItem().hasMultipleFiles())
        {
            // remove all but first bitstream from bundle[0]
            // FIXME: Assumes multiple bundles, clean up someday...
            // (only messes with the first bundle.)
            List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");

            if (bundles.size() > 0)
            {
                Iterator<Bitstream> bitstreams = bundles.get(0).getBitstreams().iterator();
                //Do NOT remove the first one
                if(bitstreams.hasNext())
                {
                    bitstreams.next();
                }

                while (bitstreams.hasNext())
                {
                    //TODO: HIBERNATE, write unit test for this
                    Bitstream bitstream = bitstreams.next();
                    bundleService.removeBitstream(context, bundles.get(0), bitstream);
                }
            }
        }
    }
}
