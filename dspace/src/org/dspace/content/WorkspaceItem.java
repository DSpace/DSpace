/*
 * WorkspaceItem.java
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;


/**
 * Class representing an item in the process of being submitted by a user
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class WorkspaceItem implements InProgressSubmission
{
    /** log4j category */
    private static Logger log = Logger.getLogger(WorkspaceItem.class);

    /** The item this workspace object pertains to */
    private Item item;
    
    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this workspace item */
    private TableRow pwRow;

    /** The collection the item is being submitted to */
    private Collection collection;


    /**
     * Construct a workspace item corresponding to the given database row
     *
     * @param context  the context this object exists in
     * @param row      the database row
     */
    WorkspaceItem(Context context, TableRow row)
        throws SQLException
    {
        ourContext = context;
        pwRow = row;

        item = Item.find(context, pwRow.getIntColumn("item_id"));
        collection = Collection.find(context,
            pwRow.getIntColumn("collection_id"));
    }


    /**
     * Get a workspace item from the database.  The item, collection and
     * submitter are loaded into memory.
     *
     * @param  context  DSpace context object
     * @param  id       ID of the workspace item
     *   
     * @return  the workspace item, or null if the ID is invalid.
     */
    public static WorkspaceItem find(Context context, int id)
        throws SQLException
    {
        TableRow row = DatabaseManager.find(context,
            "personalworkspace",
            id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_workspace_item",
                    "not_found,workspace_item_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_workspace_item",
                    "workspace_item_id=" + id));
            }

            return new WorkspaceItem(context, row);
        }
    }
    

    /**
     * Create a new workspace item, with a new ID.  An Item is also
     * created
     *
     * @param  context  DSpace context object
     * @param  coll     Collection being submitted to
     * @param  sub      The submitting e-person  
     *
     * @return  the newly created workspace item
     */
    public static WorkspaceItem create(Context context,
                                       Collection coll,
                                       EPerson sub)
        throws AuthorizeException, SQLException
    {
        // FIXME Check authorisation

        // Create an item
        Item i = Item.create(context);
        i.setSubmitter(sub);
        i.update();
        
        // Create the personal workspace row
        TableRow row = DatabaseManager.create(context, "personalworkspace");

        row.setColumn("item_id", i.getID());
        row.setColumn("collection_id", coll.getID());

        log.info(LogManager.getHeader(context,
            "create_workspace_item",
            "workspace_item_id=" + row.getIntColumn("personal_workspace_id") +
                "item_id=" + i.getID() + "collection_id=" + coll.getID()));

        DatabaseManager.update(context, row );

        return new WorkspaceItem(context, row);
    }


    /**
     * Get all workspace items for a particular e-person
     *
     * @param context   the context object
     * @param ep        the eperson
     *
     * @return  the corresponding workspace items
     */
    public static WorkspaceItem[] findByEPerson(Context context, EPerson ep)
        throws SQLException
    {
        List wsItems = new ArrayList();

        TableRowIterator tri = DatabaseManager.query(context,
            "personalworkspace",
            "SELECT personalworkspace.* FROM personalworkspace, item WHERE " +
                "personalworkspace.item_id=item.item_id AND " +
                "item.submitter_id=" + ep.getID() + ";");

        while (tri.hasNext())
        {
            TableRow row = tri.next();
            WorkspaceItem wi = new WorkspaceItem(context, row);
            wsItems.add(wi);
        }
        
        WorkspaceItem[] wsArray = new WorkspaceItem[wsItems.size()];
        wsArray = (WorkspaceItem[]) wsItems.toArray(wsArray);

        return wsArray;
    }


    /**
     * Get the internal ID of this workspace item
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return pwRow.getIntColumn("personal_workspace_id");
    }


    /**
     * Start the relevant workflow for this workspace item.  The entry in
     * PersonalWorkspace is removed, a workflow item is created, and the
     * relevant workflow initiated.  If there is no workflow, i.e. the
     * item goes straight into the archive, <code>null</code> is returned.
     * <P>
     * An accession date is assigned and a provenance description added.
     * NO license is added.
     *
     * @return   the workflow item, or <code>null</code> if the item went
     *           straight into the main archive
     */
    public WorkflowItem startWorkflow()
        throws SQLException, AuthorizeException
    {
        // FIXME Check auth

        log.info(LogManager.getHeader(ourContext,
            "start_workflow",
            "workspace_item_id=" + getID() +
                "item_id=" + item.getID() +
                "collection_id=" + collection.getID()));

        // Set accession date
        DCDate d = DCDate.getCurrent();
        item.addDC("date", "accessioned", null, d.toString());

        // Get non-internal format bitstreams
        Bitstream[] bitstreams = item.getNonInternalBitstreams();

        // Create provenance description
        String provMessage = "Submitted by" + 
            item.getSubmitter().getFullName() + " (" +
            item.getSubmitter().getEmail() + ").  DSpace accession date:" +
            d.toString() + "\n Submission has " + bitstreams.length +
            " bitstreams:\n";

        // Add sizes and checksums of bitstreams
        for (int j = 0; j < bitstreams.length; j++)
        {
            provMessage = provMessage + bitstreams[j].getName() + ": " +
                bitstreams[j].getSize() + " bytes, checksum: " +
                bitstreams[j].getChecksum() + " (" + 
                bitstreams[j].getChecksumAlgorithm() + ")\n";
        }
                    
        // Add message to the DC
        item.addDC("description", "provenance", "en", provMessage);

        return null;
    }
    

    /**
     * Update the workspace item, including the unarchived item.
     */
    public void update()
        throws SQLException, AuthorizeException
    {
        // FIXME check auth
    
        log.info(LogManager.getHeader(ourContext,
            "update_workspace_item",
            "workspace_item_id=" + getID()));

        // Update the item
        item.update();
        
        // Update ourselves
        DatabaseManager.update(ourContext, pwRow);
    }


    /**
     * Delete the workspace item.  The entry in PersonalWorkspace, the
     * unarchived item and its contents are all removed (multiple inclusion
     * notwithstanding.)
     */
    public void delete()
        throws SQLException, AuthorizeException, IOException
    {
        // FIXME Check auth
    
        log.info(LogManager.getHeader(ourContext,
            "delete_workspace_item",
            "workspace_item_id=" + getID() +
                "item_id=" + item.getID() +
                "collection_id=" + collection.getID()));

        // Need to delete the personalworkspace row first since it refers
        // to item ID
        DatabaseManager.delete(ourContext, pwRow);
        
        // Delete item
        item.deleteWithContents();
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
        return pwRow.getBooleanColumn("multiple_files");
    }

    
    public void setMultipleFiles(boolean b)
    {
        pwRow.setColumn("multiple_files", b);
    }
    

    public boolean hasMultipleTitles()
    {
        return pwRow.getBooleanColumn("multiple_titles");
    }
    

    public void setMultipleTitles(boolean b)
    {
        pwRow.setColumn("multiple_titles", b);
    }


    public boolean isPublishedBefore()
    {
        return pwRow.getBooleanColumn("published_before");
    }

    
    public void setPublishedBefore(boolean b)
    {
        pwRow.setColumn("published_before", b);
    }
}
