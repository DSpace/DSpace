/*
 * HistorySubmission.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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

package org.dspace.history;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.dspace.core.*;
import org.dspace.content.*;
import org.dspace.eperson.*;
import org.dspace.ingest.WorkspaceItem;
import org.dspace.workflow.WorkflowItem;

import org.apache.log4j.Logger;

/**
 * Bridge class between History and Workflow submission.
 * This class has the responsibility for creating history statements
 * to preserve archival information about steps in the submission process.
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class HistorySubmission
{

    /** log4j category */
    private static Logger log = Logger.getLogger(HistorySubmission.class);

    /** Private Constructor */
    private HistorySubmission () {}

    /**
     * Record the fact that user accepted the license for a
     * WorkspaceItem using tool.
     *
     * @param context Current DSpace context
     * @param item The WorkspaceItem
     * @param user The user who accepted the license
     * @param tool The tool used to accept the license
     */
    public static void userAcceptsLicense(Context context,
                                          WorkspaceItem item,
                                          EPerson user,
                                          String tool)
    {
        HistoryManager.saveHistory
        (context,
         item,
         HistoryManager.MODIFY,
         user,
         tool);
    }

    /**
     * Record the fact that reviewer approved the submission of
     * WorkflowItem using tool.
     *
     * @param context Current DSpace context
     * @param item The WorkspaceItem
     * @param reviewer The reviewer who accepted the submission
     * @param tool The tool used to accept the submission
     */
    public static void reviewerAccepts(Context context,
                                       WorkflowItem item,
                                       EPerson reviewer,
                                       String tool)
    {
        HistoryManager.saveHistory
        (context,
         item,
         HistoryManager.MODIFY,
         reviewer,
         tool);
    }

    /**
     * Record the fact that reviewer rejected the submission of
     * WorkflowItem using tool.
     *
     * @param context Current DSpace context
     * @param item The WorkspaceItem
     * @param reviewer The reviewer who rejected the submission
     * @param tool The tool used to reject the submission
     */
    public static void reviewerRejects(Context context,
                                       WorkflowItem item,
                                       EPerson reviewer,
                                       String tool)
    {
        HistoryManager.saveHistory
        (context,
         item,
         HistoryManager.REMOVE,
         reviewer,
         tool);
    }

    /**
     * Record the fact that admin accepted the submission of
     * WorkflowItem using tool.
     *
     * @param context Current DSpace context
     * @param item The WorkspaceItem
     * @param admin The admin who accepted the submission
     * @param tool The tool used to accept the submission
     */
    public static void adminAccepts(Context context,
                                    WorkflowItem item,
                                    EPerson admin,
                                    String tool)
    {
        HistoryManager.saveHistory
        (context,
         item,
         HistoryManager.MODIFY,
         admin,
         tool);
    }

    /**
     * Record the fact that admin rejected the submission of
     * WorkflowItem using tool.
     *
     * @param context Current DSpace context
     * @param item The WorkspaceItem
     * @param admin The admin who rejected the submission
     * @param tool The tool used to reject the submission
     */
    public static void adminRejects(Context context,
                                    WorkflowItem item,
                                    EPerson admin,
                                    String tool)
    {
        HistoryManager.saveHistory
        (context,
         item,
         HistoryManager.REMOVE,
         admin,
         tool);
    }

    /**
     * Record the fact that editor commits the submission of WorkflowItem
     * using tool.
     *
     * @param context Current DSpace context
     * @param item The WorkspaceItem
     * @param editor The editor who committed the submission
     * @param tool The tool used to commit the submission
     */
    public static void editorCommits(Context context,
                                     WorkflowItem item,
                                     EPerson editor,
                                     String tool)
    {
        HistoryManager.saveHistory
        (context,
         item,
         HistoryManager.MODIFY,
         editor,
         tool);
    }

    /**
     * Record the fact that the submission of item is complete.
     * The tool parameter should refer to the instrument used to complete
     * the submission, not the instrument(s) used in earlier stages
     * of the submission.
     *
     * @param context Current DSpace context
     * @param item The Item
     * @param installer The user who installed the item
     * @param tool The tool used to install the item
     */
    public static void itemInstalled(Context context,
                                     Item item,
                                     EPerson installer,
                                     String tool)
    {
        HistoryManager.saveHistory
        (context,
         item,
         HistoryManager.MODIFY,
         installer,
         tool);

        // Installing an item changes not only the item, but also the
        // collection
        try
        {
            Collection[] collections = item.getCollections();

            for (int i = 0; i < collections.length; i++ )
            {
                HistoryManager.saveHistory
                    (context,
                     collections[i],
                     HistoryManager.MODIFY,
                     installer,
                     tool);
            }
        }
        catch (SQLException sqle)
        {
            if (log.isDebugEnabled())
                log.debug("Caught SQLException " + sqle, sqle);
        }
    }
}
