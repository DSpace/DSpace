/*
 * Collection.java
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

import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.history.HistoryManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;


/**
 * Class representing a collection.
 * <P>
 * The collection's metadata (name, introductory text etc), workflow groups,
 * and default group of submitters are loaded into memory.  Changes to metadata
 * are not written to the database until <code>update</code> is called.
 * If you create or remove a workflow group, the change is only reflected in
 * the database after calling <code>update</code>.  The default group of
 * submitters is slightly different - creating or removing this has instant
 * effect.
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class Collection
{
    /** log4j category */
    private static Logger log = Logger.getLogger(Collection.class);

    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this item */
    private TableRow collectionRow;

    /** The logo bitstream */
    private Bitstream logo;

    /** The item template */
    private Item template;

    /**
     * Groups corresponding to workflow steps - NOTE these start from one,
     * so workflowGroups[0] corresponds to workflow_step_1.
     */
    private Group[] workflowGroup;

    /** The default group of submitters */
    private Group submitters;


    /**
     * Construct a collection with the given table row
     *
     * @param context  the context this object exists in
     * @param row      the corresponding row in the table
     */
    Collection(Context context, TableRow row)
        throws SQLException
    {
        ourContext = context;
        collectionRow = row;

        // Get the logo bitstream
        if (collectionRow.isColumnNull("logo_bitstream_id"))
        {
            logo = null;
        }
        else
        {
            logo = Bitstream.find(ourContext,
                collectionRow.getIntColumn("logo_bitstream_id"));
        }

        // Get the template item
        if (collectionRow.isColumnNull("template_item_id"))
        {
            template = null;
        }
        else
        {
            template = Item.find(ourContext,
                collectionRow.getIntColumn("template_item_id"));
        }

        // Get the relevant groups
        workflowGroup = new Group[3];

        workflowGroup[0] = groupFromColumn("workflow_step_1");
        workflowGroup[1] = groupFromColumn("workflow_step_2");
        workflowGroup[2] = groupFromColumn("workflow_step_3");

        // Default submitters are in a group called "COLLECTION_XX_SUBMIT"
        // where XX is the ID of this collection
        submitters = Group.findByName(ourContext,
            "COLLECTION_" + getID() + "_SUBMIT");

        // Cache ourselves
        context.cache(this, row.getIntColumn("collection_id"));
    }


    /**
     * Get a collection from the database.  Loads in the metadata
     *
     * @param  context  DSpace context object
     * @param  id       ID of the collection
     *
     * @return  the collection, or null if the ID is invalid.
     */
    public static Collection find(Context context, int id)
        throws SQLException
    {
        // First check the cache
        Collection fromCache =
            (Collection) context.fromCache(Collection.class, id);
            
        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context,
            "collection",
            id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_collection",
                    "not_found,collection_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                    "find_collection",
                    "collection_id=" + id));
            }

            return new Collection(context, row);
        }
    }


    /**
     * Create a new collection, with a new ID.  This method is not public,
     * and does not check authorisation.
     *
     * @param  context  DSpace context object
     *
     * @return  the newly created collection
     */
    static Collection create(Context context)
        throws SQLException
    {
        TableRow row = DatabaseManager.create(context, "collection");
        Collection c = new Collection(context, row);

        HistoryManager.saveHistory(context,
            c,
            HistoryManager.CREATE,
            context.getCurrentUser(),
            context.getExtraLogInfo());

        log.info(LogManager.getHeader(context,
            "create_collection",
            "collection_id=" + row.getIntColumn("collection_id")));

        return c;
    }


    /**
     * Get all collections in the system.  These are alphabetically
     * sorted by collection name.
     *
     * @param  context  DSpace context object
     *
     * @return  the collections in the system
     */
    public static Collection[] findAll(Context context)
        throws SQLException
    {
        TableRowIterator tri = DatabaseManager.query(context,
            "collection",
            "SELECT * FROM collection ORDER BY name;");

        List collections = new ArrayList();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
            Collection fromCache = (Collection) context.fromCache(
                Collection.class, row.getIntColumn("collection_id"));

            if (fromCache != null)
            {
                collections.add(fromCache);
            }
            else
            {
                collections.add(new Collection(context, row));
            }
        }

        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);

        return collectionArray;
    }


    /**
     * Get all the items in this collection.  The order is indeterminate.
     *
     * @return  an iterator over the items in the collection.
     */
    public ItemIterator getItems()
        throws SQLException
    {
        TableRowIterator rows = DatabaseManager.query(ourContext,
            "item",
            "SELECT item.* FROM item, collection2item WHERE " +
                "collection2item.collection_id=" + getID() + ";");

        return new ItemIterator(ourContext, rows);
    }


    /**
     * Get the internal ID of this collection
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return collectionRow.getIntColumn("collection_id");
    }


    /**
     * Get the value of a metadata field
     *
     * @param  field   the name of the metadata field to get
     *
     * @return  the value of the metadata field
     *
     * @exception IllegalArgumentException   if the requested metadata
     *            field doesn't exist
     */
    public String getMetadata(String field)
    {
        return collectionRow.getStringColumn(field);
    }


    /**
     * Set a metadata value
     *
     * @param  field   the name of the metadata field to get
     * @param  value   value to set the field to
     *
     * @exception IllegalArgumentException   if the requested metadata
     *            field doesn't exist
     */
    public void setMetadata(String field, String value)
    {
        collectionRow.setColumn(field, value);
    }


    /**
     * Get the logo for the collection.  <code>null</code> is return if the
     * collection does not have a logo.
     *
     * @return the logo of the collection, or <code>null</code>
     */
    public Bitstream getLogo()
    {
        return logo;
    }


    /**
     * Give the collection a logo.  Passing in <code>null</code> removes any
     * existing logo.  You will need to set the format of the new logo
     * bitstream before it will work, for example to "JPEG".  Note that
     * <code>update(/code> will need to be called for the change to take
     * effect.  Setting a logo and not calling <code>update</code> later may
     * result in a previous logo lying around as an "orphaned" bitstream.
     *
     * @param  is   the stream to use as the new logo
     *
     * @return   the new logo bitstream, or <code>null</code> if there is no
     *           logo (<code>null</code> was passed in)
     */
    public Bitstream setLogo(InputStream is)
        throws AuthorizeException, IOException, SQLException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);

        // First, delete any existing logo
        if (!collectionRow.isColumnNull("logo_bitstream_id"))
        {
            logo.delete();
        }

        if (is == null)
        {
            collectionRow.setColumnNull("logo_bitstream_id");
            logo = null;

            log.info(LogManager.getHeader(ourContext,
                "remove_logo",
                "collection_id=" + getID()));
        }
        else
        {
            Bitstream newLogo = Bitstream.create(ourContext, is);
            collectionRow.setColumn("logo_bitstream_id", newLogo.getID());
            logo = newLogo;

            log.info(LogManager.getHeader(ourContext,
                "set_logo",
                "collection_id=" + getID() +
                    "logo_bitstream_id=" + newLogo.getID()));
        }

        return logo;
    }


    /**
     * Create a workflow group for the given step if one does not already exist.
     * Returns either the newly created group or the previously existing one.
     * Note that while the new group is created in
     * the database, the association between the group and the collection
     * is not written until <code>update</code> is called.
     *
     * @param   step  the step (1-3) of the workflow to create or get the group
     *                for
     *
     * @return  the workflow group associated with this collection
     */
    public Group createWorkflowGroup(int step)
        throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);

        if (workflowGroup[step-1] == null)
        {
            workflowGroup[step-1] = Group.create(ourContext);
            workflowGroup[step-1].setName(
                "COLLECTION_" + getID() + "_WORKFLOW_STEP_" + step);
            workflowGroup[step-1].update();
        }

        return workflowGroup[step-1];
    }


    /**
     * Set the workflow group corresponding to a particular workflow step.
     * <code>null</code> can be passed in if there should be no associated
     * group for that workflow step; any existing group is NOT deleted.
     *
     * @param   step   the workflow step (1-3)
     * @param   g      the new workflow group, or <code>null</code>
     */
    public void setWorkflowGroup(int step, Group g)
    {
        workflowGroup[step-1] = g;
    }


    /**
     * Get the the workflow group corresponding to a particular workflow step.
     * This returns <code>null</code> if there is no group associated with this
     * collection for the given step.
     *
     * @param   step   the workflow step (1-3)
     *
     * @return  the group of reviewers or <code>null</code>
     */
    public Group getWorkflowGroup(int step)
    {
        return workflowGroup[step-1];
    }


    /**
     * Create a default submitters group if one does not already exist.
     * Returns either the newly created group or the previously existing one.
     * Note that other groups may also be allowed to submit to this collection
     * by the authorization system.
     *
     * @return  the default group of submitters associated with this collection
     */
    public Group createSubmitters()
        throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);

        if (submitters == null)
        {
            submitters = Group.create(ourContext);
            submitters.setName("COLLECTION_" + getID() + "_SUBMIT");
            submitters.update();
        }

        return submitters;
    }


    /**
     * Get the default group of submitters, if there is one.  Note that the
     * authorization system may allow others to submit to the collection, so
     * this is not necessarily a definitive list of potential submitters.
     * <P>
     * The default group of submitters for collection 100 is the one called
     * <code>collection_100_submit</code>.
     *
     * @return  the default group of submitters, or <code>null</code> if
     *          there is no default group.
     */
    public Group getSubmitters()
    {
        return submitters;
    }


    /**
     * Get the license that users must grant before submitting to this
     * collection.  If the collection does not have a specific license,
     * the site-wide default is returned.
     *
     * @return  the license for this collection
     */
    public String getLicense()
    {
        String license = collectionRow.getStringColumn("license");

        if (license == null || license.equals(""))
        {
            // Fallback to site-wide default
            license = ConfigurationManager.getDefaultSubmissionLicense();
        }

        return license;
    }


    /**
     * Find out if the collection has a custom license
     *
     * @return  <code>true</code> if the collection has a custom license
     */
    public boolean hasCustomLicense()
    {
        String license = collectionRow.getStringColumn("license");

        return (license != null && !license.equals(""));
    }


    /**
     * Set the license for this collection.  Passing in <code>null</code>
     * means that the site-wide default will be used.
     *
     * @param  license  the license, or <code>null</code>
     */
    public void setLicense(String license)
    {
        if (license == null)
        {
            collectionRow.setColumnNull("license");
        }
        else
        {
            collectionRow.setColumn("license", license);
        }
    }


    /**
     * Get the template item for this collection.  <code>null</code> is returned
     * if the collection does not have a template.  Submission mechanisms
     * may copy this template to provide a convenient starting point for
     * a submission.
     *
     * @return  the item template, or <code>null</code>
     */
    public Item getTemplateItem()
        throws SQLException
    {
        return template;
    }


    /**
     * Create an empty template item for this collection.  If one already
     * exists, no action is taken.  Caution:  Make sure you call
     * <code>update</code> on the collection after doing this, or the item
     * will have been created but the collection record will not refer to it.
     */
    public void createTemplateItem()
        throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);

        if (template == null)
        {
            template = Item.create(ourContext);

            log.info(LogManager.getHeader(ourContext,
                "create_template_item",
                "collection_id=" + getID() +
                    ",template_item_id=" + template.getID()));
        }
    }


    /**
     * Remove the template item for this collection, if there is one.  Note
     * that since this has to remove the old template item ID from the
     * collection record in the database, the colletion record will be changed,
     * including any other changes made; in other words, this method does
     * an <code>update</code>.
     */
    public void removeTemplateItem()
        throws SQLException, AuthorizeException, IOException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);

        collectionRow.setColumnNull("template_item_id");
        DatabaseManager.update(ourContext, collectionRow);

        if (template != null)
        {
            log.info(LogManager.getHeader(ourContext,
                "remove_template_item",
                "collection_id=" + getID() +
                    ",template_item_id=" + template.getID()));

            template.delete();
            template = null;
        }
    }


    /**
     * Add an item to the collection.  This simply adds a relationship between
     * the item and the collection - it does nothing like set an issue date,
     * remove a personal workspace item etc.  This has instant effect;
     * <code>update</code> need not be called.
     *
     * @param item  item to add
     */
    public void addItem(Item item)
        throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        log.info(LogManager.getHeader(ourContext,
            "add_item",
            "collection_id=" + getID() +
                ",item_id=" + item.getID()));

        // Create mapping
        TableRow row = DatabaseManager.create(ourContext, "collection2item");

        row.setColumn("collection_id", getID());
        row.setColumn("item_id", item.getID());

        DatabaseManager.update(ourContext, row);
    }


    /**
     * Remove an item.  If the item is then orphaned, it is deleted.
     *
     * @param item  item to remove
     */
    public void removeItem(Item item)
        throws SQLException, AuthorizeException,IOException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        log.info(LogManager.getHeader(ourContext,
            "remove_item",
            "collection_id=" + getID() + ",item_id=" + item.getID()));

        DatabaseManager.updateQuery(ourContext,
            "DELETE FROM collection2item WHERE collection_id=" + getID() +
            " AND item_id=" + item.getID() + ";");

        // Is the item an orphan?
        TableRowIterator tri = DatabaseManager.query(ourContext,
            "SELECT * FROM collection2item WHERE item_id=" +
                item.getID());

        if (!tri.hasNext())
        {
            // Orphan; delete it
            item.delete();
        }
    }


    /**
     * Update the collection metadata (including logo, and workflow groups)
     * to the database.  Inserts if this is a new collection.
     */
    public void update()
        throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);

        HistoryManager.saveHistory(ourContext,
            this,
            HistoryManager.MODIFY,
            ourContext.getCurrentUser(),
            ourContext.getExtraLogInfo());

        log.info(LogManager.getHeader(ourContext,
            "update_collection",
            "collection_id=" + getID()));

        DatabaseManager.update(ourContext, collectionRow);
    }


    /**
     * Delete the collection, including the metadata and logo.  Items that
     * are then orphans are deleted.
     * Groups associated with this collection (workflow
     * participants and submitters) are NOT deleted.
     */
    void delete()
        throws SQLException, AuthorizeException, IOException
    {
        log.info(LogManager.getHeader(ourContext,
            "delete_collection",
            "collection_id=" + getID()));

        // Remove from cache
        ourContext.removeCached(this, getID());

        HistoryManager.saveHistory(ourContext,
            this,
            HistoryManager.REMOVE,
            ourContext.getCurrentUser(),
            ourContext.getExtraLogInfo());

        // Remove items
        ItemIterator items = getItems();
        
        while (items.hasNext())
        {
            removeItem(items.next());
        }

        // Delete bitstream logo
        setLogo(null);

        // Delete collection row
        DatabaseManager.delete(ourContext, collectionRow);

        // FIXME: Groups?
    }


    /**
     * Get the communities this collection appears in
     *
     * @return   array of <code>Community</code> objects
     */
    public Community[] getCommunities()
        throws SQLException
    {
        // Get the bundle table rows
        TableRowIterator tri = DatabaseManager.query(ourContext,
            "community",
            "SELECT community.* FROM community, community2collection WHERE " +
                "community.community_id=community2collection.community_id " +
                "AND community2collection.collection_id=" +
                getID() + ";");

        // Build a list of Community objects
        List communities = new ArrayList();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
            Community fromCache = (Community) ourContext.fromCache(
                Community.class, row.getIntColumn("community_id"));

            if (fromCache != null)
            {
                communities.add(fromCache);
            }
            else
            {
                communities.add(new Community(ourContext, row));
            }
        }

        Community[] communityArray = new Community[communities.size()];
        communityArray = (Community[]) communities.toArray(communityArray);

        return communityArray;
    }


    /**
     * Return <code>true</code> if <code>other</code> is the same Collection as
     * this object, <code>false</code> otherwise
     *
     * @param other   object to compare to
     *
     * @return  <code>true</code> if object passed in represents the same
     *          collection as this object
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof Collection))
        {
            return false;
        }

        return (getID() == ((Collection) other).getID());
    }


    /**
     * Utility method for reading in a group from a group ID in a column.
     * If the column is null, null is returned.
     *
     * @param col    the column name to read
     * @return    the group referred to by that column, or null
     */

    private Group groupFromColumn(String col)
        throws SQLException
    {
        if (collectionRow.isColumnNull(col))
        {
            return null;
        }
        else
        {
            return Group.find(ourContext, collectionRow.getIntColumn(col));
        }
    }
}
