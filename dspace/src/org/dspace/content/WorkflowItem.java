/*
 * WorkflowItem.java
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

package org.dspace.content;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;


/**
 * Class representing an item going through the workflow process in DSpace
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class WorkflowItem implements InProgressSubmission
{
    /**
     * Get a workflow item from the database.  The item, collection and
     * submitter are loaded into memory.
     *
     * @param  context  DSpace context object
     * @param  id       ID of the workspace item
     *   
     * @return  the workflow item, or null if the ID is invalid.
     */
    public static WorkflowItem find(Context context, int id)
        throws SQLException
    {
        return null;
    }


    /**
     * Return the workflow item to the personal workspace of the submitter.
     * The workflow item is removed, and a workspace item created.
     * 
     * @return  the personal workspace item
     */
    public WorkspaceItem returnToWorkspace()
        throws SQLException, AuthorizeException
    {
        return null;
    }

    
    /**
     * Commit the contained item to the main archive.  The item is
     * associated with the relevant collection, added to the search index,
     * and any other tasks such as assigning dates are performed.
     *
     * @return  the fully archived item.
     */
    public Item archive()
        throws SQLException, AuthorizeException
    {
        return null;
    }


    // InProgressSubmission methods

    public Item getItem()
    {
        return null;
    }
    

    public Collection getCollection()
    {
        return null;
    }

    
    public EPerson getSubmitter()
    {
        return null;
    }
    

    public boolean hasMultipleFiles()
    {
        return false;
    }

    
    public void setMultipleFiles(boolean b)
        throws AuthorizeException
    {
    }
    

    public boolean hasMultipleTitles()
    {
        return false;
    }
    

    public void setMultipleTitles(boolean b)
        throws AuthorizeException
    {
    }


    public boolean isPublishedBefore()
    {
        return false;
    }

    
    public void setPublishedBefore(boolean b)
        throws AuthorizeException
    {
    }
}
