/*
 * SkipInitialQuestionsStep.java
 *
 * Version: $Revision: 3738 $
 *
 * Date: $Date: 2009-04-24 00:32:12 -0400 (Fri, 24 Apr 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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
package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
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
 * @see org.dspace.submit.step.InitialQuestionStep
 * @see org.dspace.submit.step.DescribeStep
 * 
 * @author Andrea Bollini
 * @version $Revision: 3738 $
 */
public class SkipInitialQuestionsStep extends AbstractProcessingStep
{
    /**
     * Simply we flags the submission as the user had checked both multi-title,
     * multi-files and published before so that the input-form configuration
     * will be used as is
     */
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        InProgressSubmission submissionItem = subInfo.getSubmissionItem();
        submissionItem.setMultipleFiles(true);
        submissionItem.setMultipleTitles(true);
        submissionItem.setPublishedBefore(true);
        submissionItem.update();
        return STATUS_COMPLETE;
    }

    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        return 1;
    }
}
