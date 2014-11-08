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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

/**
 * This is a Simple Step class that need to be used when you want skip the
 * initial questions step!
 * <p>
 * At the moment this step is required because part of the behaviour of the
 * InitialQuestionStep is required to be managed also in the DescribeStep (see
 * JIRA [DS-83] Hardcoded behaviour of Initial question step in the submission)
 * </p>
 * 
 * @see org.dspace.submit.AbstractProcessingStep
 * @see org.dspace.submit.step.InitialQuestionsStep
 * @see org.dspace.submit.step.DescribeStep
 * 
 * @author Andrea Bollini
 * @version $Revision$
 */
public class SkipInitialQuestionsStep extends AbstractProcessingStep
{
    /**
     * Simply we flags the submission as the user had checked both multi-title,
     * multi-files and published before so that the input-form configuration
     * will be used as is
     */
    @Override
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        InProgressSubmission submissionItem = subInfo.getSubmissionItem();
        submissionItem.setMultipleFiles(true);
        submissionItem.setMultipleTitles(true);
        submissionItem.setPublishedBefore(true);
        ContentServiceFactory.getInstance().getInProgressSubmissionService(submissionItem).update(context, submissionItem);
        return STATUS_COMPLETE;
    }

    @Override
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        return 1;
    }
}
