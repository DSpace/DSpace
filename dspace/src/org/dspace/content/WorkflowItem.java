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

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;


/**
 * Class representing an item going through the workflow process in DSpace
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class WorkflowItem implements InProgressSubmission
{
    /** log4j category */
    private static Logger log = Logger.getLogger(WorkflowItem.class);

    /** The item this workflow object pertains to */
    private Item item;
    
    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this workflow item */
    private TableRow wfRow;

    /** The collection the item is being submitted to */
    private Collection collection;


    /**
     * Construct a workspace item corresponding to the given database row
     *
     * @param context  the context this object exists in
     * @param row      the database row
     */
    WorkflowItem(Context context, TableRow row)
        throws SQLException
    {
        ourContext = context;
        wfRow = row;

        item = Item.find(context, wfRow.getIntColumn("item_id"));
        collection = Collection.find(context,
            wfRow.getIntColumn("collection_id"));
    }


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
        TableRow row = DatabaseManager.find(context,
            "workflowitem",
            id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_workflow_item",
                    "not_found,workflow_item_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_workflow_item",
                    "workflow_item_id=" + id));
            }

            return new WorkflowItem(context, row);
        }
    }


    /**
     * Get the internal ID of this workflow item
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return wfRow.getIntColumn("workflow_id");
    }


    /**
     * Return the workflow item to the workspace of the submitter.
     * The workflow item is removed, and a workspace item created.
     * 
     * @return  the workspace item
     */
    public WorkspaceItem returnToWorkspace()
        throws SQLException, AuthorizeException
    {
        // FIXME: How should this interact with the workflow system?
        // FIXME: Remove license
        // FIXME: Provenance statement?
        
        // Remove accession date
        item.clearDC("date", "accessioned", Item.ANY);
        item.update();

        // Create the new workspace item row
        TableRow row = DatabaseManager.create(ourContext, "workspaceitem");
        row.setColumn("item_id", item.getID());
        row.setColumn("collection_id", collection.getID());

        WorkspaceItem wi = new WorkspaceItem(ourContext, row);
        wi.setMultipleFiles(hasMultipleFiles());
        wi.setMultipleTitles(hasMultipleTitles());
        wi.setPublishedBefore(isPublishedBefore());
        wi.update();
        
        log.info(LogManager.getHeader(ourContext,
            "return_to_workspace",
            "workflow_item_id=" + getID() + "workspace_item_id=" + wi.getID()));

        // Now remove the workflow object
        DatabaseManager.delete(ourContext, wfRow);

        return wi;
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
        // FIXME: Check auth

        log.info(LogManager.getHeader(ourContext,
            "archive_item",
            "workflow_item_id=" + getID() + "item_id=" + item.getID() +
                "collection_id=" + collection.getID()));

        // Remove workflow item
        DatabaseManager.delete(ourContext, wfRow);

        // Add item to collection
        collection.addItem(item);

        // FIXME: Assign handle
        // FIXME: Add handle as identifier.uri DC value

        // Add format.mimetype and format.extent DC values
        Bitstream[] bitstreams = item.getNonInternalBitstreams();
        
        for (int i = 0; i < bitstreams.length; i++)
        {
            BitstreamFormat bf = bitstreams[i].getFormat();
            item.addDC("format",
                "extent",
                null,
                String.valueOf(bitstreams[i].getSize()));
            item.addDC("format", "mimetype", null, bf.getMIMEType());
        }

        // Assign issue date, if none exists, and build up provenance
        DCDate now = DCDate.getCurrent();

        DCValue[] currentDateIssued = item.getDC("date",
            "issued",
            null);

        String provDescription = "Made available in DSpace on " + now +
            " (GMT).";

        if (currentDateIssued.length == 0)
        {
            item.addDC("date", "issued", null, now.toString());
        }
        else
        {
            DCDate d = new DCDate(currentDateIssued[0].value);
            provDescription = provDescription + "  Previous issue date: " +
                d.toString();
        }

        // Add provenance description
        item.addDC("description", "provenance", "en", provDescription);

        // Set in_archive bit
        item.setArchived(true);
        item.update();
        
        // Log the event
        log.info(LogManager.getHeader(
            ourContext,
            "install_item",
            "workflow_id=" + getID() + ", item_id=" + item.getID() +
            "handle=FIXME"));

        return item;
    }


    /**
     * Update the workflow item, including the unarchived item.
     */
    public void update()
        throws SQLException, AuthorizeException
    {
        // FIXME check auth
    
        log.info(LogManager.getHeader(ourContext,
            "update_workflow_item",
            "workflow_item_id=" + getID()));

        // Update the item
        item.update();
        
        // Update ourselves
        DatabaseManager.update(ourContext, wfRow);
    }


    // InProgressSubmission methods

    public Item getItem()
    {
        return item;
    }
    

    public Collection getCollection()
    {
        return collection;
    }

    
    public EPerson getSubmitter()
    {
        return item.getSubmitter();
    }
    

    public boolean hasMultipleFiles()
    {
        return wfRow.getBooleanColumn("multiple_files");
    }

    
    public void setMultipleFiles(boolean b)
    {
        wfRow.setColumn("multiple_files", b);
    }
    

    public boolean hasMultipleTitles()
    {
        return wfRow.getBooleanColumn("multiple_titles");
    }
    

    public void setMultipleTitles(boolean b)
    {
        wfRow.setColumn("multiple_titles", b);
    }


    public boolean isPublishedBefore()
    {
        return wfRow.getBooleanColumn("published_before");
    }

    
    public void setPublishedBefore(boolean b)
    {
        wfRow.setColumn("published_before", b);
    }
}
