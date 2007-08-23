/*
 * Collection.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.workflow.WorkflowItem;

/**
 * Class representing a collection.
 * <P>
 * The collection's metadata (name, introductory text etc), workflow groups, and
 * default group of submitters are loaded into memory. Changes to metadata are
 * not written to the database until <code>update</code> is called. If you
 * create or remove a workflow group, the change is only reflected in the
 * database after calling <code>update</code>. The default group of
 * submitters is slightly different - creating or removing this has instant
 * effect.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class Collection extends DSpaceObject
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

    /** Our Handle */
    private String handle;

    /** Flag set when data is modified, for events */
    private boolean modified;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;

    /**
     * Groups corresponding to workflow steps - NOTE these start from one, so
     * workflowGroups[0] corresponds to workflow_step_1.
     */
    private Group[] workflowGroup;

    /** The default group of submitters */
    private Group submitters;

    /** The default group of administrators */
    private Group admins;

    /**
     * Construct a collection with the given table row
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     * @throws SQLException
     */
    Collection(Context context, TableRow row) throws SQLException
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
            logo = Bitstream.find(ourContext, collectionRow
                    .getIntColumn("logo_bitstream_id"));
        }

        // Get the template item
        if (collectionRow.isColumnNull("template_item_id"))
        {
            template = null;
        }
        else
        {
            template = Item.find(ourContext, collectionRow
                    .getIntColumn("template_item_id"));
        }

        // Get the relevant groups
        workflowGroup = new Group[3];

        workflowGroup[0] = groupFromColumn("workflow_step_1");
        workflowGroup[1] = groupFromColumn("workflow_step_2");
        workflowGroup[2] = groupFromColumn("workflow_step_3");

        submitters = groupFromColumn("submitter");
        admins = groupFromColumn("admin");
        
        // Get our Handle if any
        handle = HandleManager.findHandle(context, this);

        // Cache ourselves
        context.cache(this, row.getIntColumn("collection_id"));

        modified = modifiedMetadata = false;
        clearDetails();
    }

    /**
     * Get a collection from the database. Loads in the metadata
     * 
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the collection
     * 
     * @return the collection, or null if the ID is invalid.
     * @throws SQLException
     */
    public static Collection find(Context context, int id) throws SQLException
    {
        // First check the cache
        Collection fromCache = (Collection) context.fromCache(Collection.class,
                id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "collection", id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_collection",
                        "not_found,collection_id=" + id));
            }

            return null;
        }

        // not null, return Collection
        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader(context, "find_collection",
                    "collection_id=" + id));
        }

        return new Collection(context, row);
    }

    /**
     * Create a new collection, with a new ID. This method is not public, and
     * does not check authorisation.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the newly created collection
     * @throws SQLException
     * @throws AuthorizeException
     */
    static Collection create(Context context) throws SQLException,
            AuthorizeException
    {
        TableRow row = DatabaseManager.create(context, "collection");
        Collection c = new Collection(context, row);
        c.handle = HandleManager.createHandle(context, c);

        // create the default authorization policy for collections
        // of 'anonymous' READ
        Group anonymousGroup = Group.find(context, 0);

        ResourcePolicy myPolicy = ResourcePolicy.create(context);
        myPolicy.setResource(c);
        myPolicy.setAction(Constants.READ);
        myPolicy.setGroup(anonymousGroup);
        myPolicy.update();

        // now create the default policies for submitted items
        myPolicy = ResourcePolicy.create(context);
        myPolicy.setResource(c);
        myPolicy.setAction(Constants.DEFAULT_ITEM_READ);
        myPolicy.setGroup(anonymousGroup);
        myPolicy.update();

        myPolicy = ResourcePolicy.create(context);
        myPolicy.setResource(c);
        myPolicy.setAction(Constants.DEFAULT_BITSTREAM_READ);
        myPolicy.setGroup(anonymousGroup);
        myPolicy.update();

        context.addEvent(new Event(Event.CREATE, Constants.COLLECTION, c.getID(), c.handle));

        log.info(LogManager.getHeader(context, "create_collection",
                "collection_id=" + row.getIntColumn("collection_id"))
                + ",handle=" + c.handle);

        return c;
    }

    /**
     * Get all collections in the system. These are alphabetically sorted by
     * collection name.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the collections in the system
     * @throws SQLException
     */
    public static Collection[] findAll(Context context) throws SQLException
    {
        TableRowIterator tri = DatabaseManager.queryTable(context, "collection",
                "SELECT * FROM collection ORDER BY name");

        List<Collection> collections = new ArrayList<Collection>();

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
        // close the TableRowIterator to free up resources
        tri.close();

        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);

        return collectionArray;
    }

    /**
     * Get the in_archive items in this collection. The order is indeterminate.
     * 
     * @return an iterator over the items in the collection.
     * @throws SQLException
     */
    public ItemIterator getItems() throws SQLException
    {
        String myQuery = "SELECT item.* FROM item, collection2item WHERE "
                + "item.item_id=collection2item.item_id AND "
                + "collection2item.collection_id= ? "
                + "AND item.in_archive='1'";

        TableRowIterator rows = DatabaseManager.queryTable(ourContext, "item",
                myQuery,getID());

        return new ItemIterator(ourContext, rows);
    }

    /**
     * Get all the items in this collection. The order is indeterminate.
     * 
     * @return an iterator over the items in the collection.
     * @throws SQLException
     */
    public ItemIterator getAllItems() throws SQLException
    {
        String myQuery = "SELECT item.* FROM item, collection2item WHERE "
                + "item.item_id=collection2item.item_id AND "
                + "collection2item.collection_id= ? ";

        TableRowIterator rows = DatabaseManager.queryTable(ourContext, "item",
                myQuery,getID());

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
     * @see org.dspace.content.DSpaceObject#getHandle()
     */
    public String getHandle()
    {
        if(handle == null) {
        	try {
				handle = HandleManager.findHandle(this.ourContext, this);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
        }
    	return handle;
    }

    /**
     * Get the value of a metadata field
     * 
     * @param field
     *            the name of the metadata field to get
     * 
     * @return the value of the metadata field
     * 
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     */
    public String getMetadata(String field)
    {
    	String metadata = collectionRow.getStringColumn(field);
    	return (metadata == null) ? "" : metadata; 
    }

    /**
     * Set a metadata value
     * 
     * @param field
     *            the name of the metadata field to get
     * @param value
     *            value to set the field to
     * 
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     * @exception MissingResourceException
     */
    public void setMetadata(String field, String value) throws MissingResourceException
    {
        if ((field.trim()).equals("name") && (value.trim()).equals(""))
        {
            try
            {
                value = I18nUtil.getMessage("org.dspace.workflow.WorkflowManager.untitled");
            }
            catch (MissingResourceException e)
            {
                value = "Untitled";
            }
        }
        collectionRow.setColumn(field, value);
        modifiedMetadata = true;
        addDetails(field);
    }

    public String getName()
    {
        return getMetadata("name");
    }

    /**
     * Get the logo for the collection. <code>null</code> is return if the
     * collection does not have a logo.
     * 
     * @return the logo of the collection, or <code>null</code>
     */
    public Bitstream getLogo()
    {
        return logo;
    }

    /**
     * Give the collection a logo. Passing in <code>null</code> removes any
     * existing logo. You will need to set the format of the new logo bitstream
     * before it will work, for example to "JPEG". Note that
     * <code>update(/code> will need to be called for the change to take
     * effect.  Setting a logo and not calling <code>update</code> later may
     * result in a previous logo lying around as an "orphaned" bitstream.
     *
     * @param  is   the stream to use as the new logo
     *
     * @return   the new logo bitstream, or <code>null</code> if there is no
     *           logo (<code>null</code> was passed in)
     * @throws AuthorizeException
     * @throws IOException
     * @throws SQLException
     */
    public Bitstream setLogo(InputStream is) throws AuthorizeException,
            IOException, SQLException
    {
        // Check authorisation
        // authorized to remove the logo when DELETE rights
        // authorized when canEdit
        if (!((is == null) && AuthorizeManager.authorizeActionBoolean(
                ourContext, this, Constants.DELETE)))
        {
            canEdit();
        }

        // First, delete any existing logo
        if (!collectionRow.isColumnNull("logo_bitstream_id"))
        {
            logo.delete();
        }

        if (is == null)
        {
            collectionRow.setColumnNull("logo_bitstream_id");
            logo = null;

            log.info(LogManager.getHeader(ourContext, "remove_logo",
                    "collection_id=" + getID()));
        }
        else
        {
            Bitstream newLogo = Bitstream.create(ourContext, is);
            collectionRow.setColumn("logo_bitstream_id", newLogo.getID());
            logo = newLogo;

            // now create policy for logo bitstream
            // to match our READ policy
            List policies = AuthorizeManager.getPoliciesActionFilter(
                    ourContext, this, Constants.READ);
            AuthorizeManager.addPolicies(ourContext, policies, newLogo);

            log.info(LogManager.getHeader(ourContext, "set_logo",
                    "collection_id=" + getID() + "logo_bitstream_id="
                            + newLogo.getID()));
        }

        modified = true;
        return logo;
    }

    /**
     * Create a workflow group for the given step if one does not already exist.
     * Returns either the newly created group or the previously existing one.
     * Note that while the new group is created in the database, the association
     * between the group and the collection is not written until
     * <code>update</code> is called.
     * 
     * @param step
     *            the step (1-3) of the workflow to create or get the group for
     * 
     * @return the workflow group associated with this collection
     * @throws SQLException
     * @throws AuthorizeException
     */
    public Group createWorkflowGroup(int step) throws SQLException,
            AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);

        if (workflowGroup[step - 1] == null)
        {
            Group g = Group.create(ourContext);
            g.setName("COLLECTION_" + getID() + "_WORKFLOW_STEP_" + step);
            g.update();
            setWorkflowGroup(step, g);

            AuthorizeManager.addPolicy(ourContext, this, Constants.ADD, g);
        }

        return workflowGroup[step - 1];
    }

    /**
     * Set the workflow group corresponding to a particular workflow step.
     * <code>null</code> can be passed in if there should be no associated
     * group for that workflow step; any existing group is NOT deleted.
     * 
     * @param step
     *            the workflow step (1-3)
     * @param g
     *            the new workflow group, or <code>null</code>
     */
    public void setWorkflowGroup(int step, Group g)
    {
        workflowGroup[step - 1] = g;

        if (g == null)
        {
            collectionRow.setColumnNull("workflow_step_" + step);
        }
        else
        {
            collectionRow.setColumn("workflow_step_" + step, g.getID());
        }
        modified = true;
    }

    /**
     * Get the the workflow group corresponding to a particular workflow step.
     * This returns <code>null</code> if there is no group associated with
     * this collection for the given step.
     * 
     * @param step
     *            the workflow step (1-3)
     * 
     * @return the group of reviewers or <code>null</code>
     */
    public Group getWorkflowGroup(int step)
    {
        return workflowGroup[step - 1];
    }

    /**
     * Create a default submitters group if one does not already exist. Returns
     * either the newly created group or the previously existing one. Note that
     * other groups may also be allowed to submit to this collection by the
     * authorization system.
     * 
     * @return the default group of submitters associated with this collection
     * @throws SQLException
     * @throws AuthorizeException
     */
    public Group createSubmitters() throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);

        if (submitters == null)
        {
            submitters = Group.create(ourContext);
            submitters.setName("COLLECTION_" + getID() + "_SUBMIT");
            submitters.update();
        }

        // register this as the submitter group
        collectionRow.setColumn("submitter", submitters.getID());
        
        AuthorizeManager.addPolicy(ourContext, this, Constants.ADD, submitters);

        modified = true;
        return submitters;
    }

    /**
     * Get the default group of submitters, if there is one. Note that the
     * authorization system may allow others to submit to the collection, so
     * this is not necessarily a definitive list of potential submitters.
     * <P>
     * The default group of submitters for collection 100 is the one called
     * <code>collection_100_submit</code>.
     * 
     * @return the default group of submitters, or <code>null</code> if there
     *         is no default group.
     */
    public Group getSubmitters()
    {
        return submitters;
    }

    /**
     * Create a default administrators group if one does not already exist.
     * Returns either the newly created group or the previously existing one.
     * Note that other groups may also be administrators.
     * 
     * @return the default group of editors associated with this collection
     * @throws SQLException
     * @throws AuthorizeException
     */
    public Group createAdministrators() throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);

        if (admins == null)
        {
            admins = Group.create(ourContext);
            admins.setName("COLLECTION_" + getID() + "_ADMIN");
            admins.update();
        }

        AuthorizeManager.addPolicy(ourContext, this,
                Constants.COLLECTION_ADMIN, admins);

        // register this as the admin group
        collectionRow.setColumn("admin", admins.getID());
        
        // administrators also get ADD on the submitter group
        if (submitters != null)
        {
            AuthorizeManager.addPolicy(ourContext, submitters, Constants.ADD,
                    admins);
        }

        modified = true;
        return admins;
    }

    /**
     * Get the default group of administrators, if there is one. Note that the
     * authorization system may allow others to be administrators for the
     * collection.
     * <P>
     * The default group of administrators for collection 100 is the one called
     * <code>collection_100_admin</code>.
     * 
     * @return group of administrators, or <code>null</code> if there is no
     *         default group.
     */
    public Group getAdministrators()
    {
        return admins;
    }

    /**
     * Get the license that users must grant before submitting to this
     * collection. If the collection does not have a specific license, the
     * site-wide default is returned.
     * 
     * @return the license for this collection
     */
    public String getLicense()
    {
        String license = collectionRow.getStringColumn("license");

        if ((license == null) || license.equals(""))
        {
            // Fallback to site-wide default
            license = ConfigurationManager.getDefaultSubmissionLicense();
        }

        return license;
    }

    /**
     * Get the license that users must grant before submitting to this
     * collection. 
     * 
     * @return the license for this collection
     */
    public String getLicenseCollection()
    {
        String license = collectionRow.getStringColumn("license");
        return license;
    }

    /**
     * Find out if the collection has a custom license
     * 
     * @return <code>true</code> if the collection has a custom license
     */
    public boolean hasCustomLicense()
    {
        String license = collectionRow.getStringColumn("license");

        return ((license != null) && !license.equals(""));
    }

    /**
     * Set the license for this collection. Passing in <code>null</code> means
     * that the site-wide default will be used.
     * 
     * @param license
     *            the license, or <code>null</code>
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
        modified = true;
    }

    /**
     * Get the template item for this collection. <code>null</code> is
     * returned if the collection does not have a template. Submission
     * mechanisms may copy this template to provide a convenient starting point
     * for a submission.
     * 
     * @return the item template, or <code>null</code>
     */
    public Item getTemplateItem() throws SQLException
    {
        return template;
    }

    /**
     * Create an empty template item for this collection. If one already exists,
     * no action is taken. Caution: Make sure you call <code>update</code> on
     * the collection after doing this, or the item will have been created but
     * the collection record will not refer to it.
     * 
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void createTemplateItem() throws SQLException, AuthorizeException
    {
        // Check authorisation
        canEdit();

        if (template == null)
        {
            template = Item.create(ourContext);
            collectionRow.setColumn("template_item_id", template.getID());

            log.info(LogManager.getHeader(ourContext, "create_template_item",
                    "collection_id=" + getID() + ",template_item_id="
                            + template.getID()));
        }
        modified = true;
    }

    /**
     * Remove the template item for this collection, if there is one. Note that
     * since this has to remove the old template item ID from the collection
     * record in the database, the colletion record will be changed, including
     * any other changes made; in other words, this method does an
     * <code>update</code>.
     * 
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeTemplateItem() throws SQLException, AuthorizeException,
            IOException
    {
        // Check authorisation
        canEdit();

        collectionRow.setColumnNull("template_item_id");
        DatabaseManager.update(ourContext, collectionRow);

        if (template != null)
        {
            log.info(LogManager.getHeader(ourContext, "remove_template_item",
                    "collection_id=" + getID() + ",template_item_id="
                            + template.getID()));

            template.delete();
            template = null;
        }
        ourContext.addEvent(new Event(Event.MODIFY, Constants.COLLECTION, getID(), "remove_template_item"));
    }

    /**
     * Add an item to the collection. This simply adds a relationship between
     * the item and the collection - it does nothing like set an issue date,
     * remove a personal workspace item etc. This has instant effect;
     * <code>update</code> need not be called.
     * 
     * @param item
     *            item to add
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void addItem(Item item) throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        log.info(LogManager.getHeader(ourContext, "add_item", "collection_id="
                + getID() + ",item_id=" + item.getID()));

        // Create mapping
        TableRow row = DatabaseManager.create(ourContext, "collection2item");

        row.setColumn("collection_id", getID());
        row.setColumn("item_id", item.getID());

        DatabaseManager.update(ourContext, row);

        ourContext.addEvent(new Event(Event.ADD, Constants.COLLECTION, getID(), Constants.ITEM, item.getID(), item.getHandle()));
    }

    /**
     * Remove an item. If the item is then orphaned, it is deleted.
     * 
     * @param item
     *            item to remove
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeItem(Item item) throws SQLException, AuthorizeException,
            IOException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        log.info(LogManager.getHeader(ourContext, "remove_item",
                "collection_id=" + getID() + ",item_id=" + item.getID()));

        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM collection2item WHERE collection_id= ? "+
                "AND item_id= ? ",
                getID(), item.getID());

        ourContext.addEvent(new Event(Event.REMOVE, Constants.COLLECTION, getID(), Constants.ITEM, item.getID(), item.getHandle()));

        // Is the item an orphan?
        TableRowIterator tri = DatabaseManager.query(ourContext,
                "SELECT * FROM collection2item WHERE item_id= ? ",
                item.getID());

        if (!tri.hasNext())
        {
            //make the right to remove the item explicit because the implicit
            // relation
            //has been removed. This only has to concern the currentUser
            // because
            //he started the removal process and he will end it too.
            //also add right to remove from the item to remove it's bundles.
            AuthorizeManager.addPolicy(ourContext, item, Constants.DELETE,
                    ourContext.getCurrentUser());
            AuthorizeManager.addPolicy(ourContext, item, Constants.REMOVE,
                    ourContext.getCurrentUser());

            // Orphan; delete it
            item.delete();
        }
        // close the TableRowIterator to free up resources
        tri.close();
    }

    /**
     * Update the collection metadata (including logo, and workflow groups) to
     * the database. Inserts if this is a new collection.
     * 
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public void update() throws SQLException, IOException, AuthorizeException
    {
        // Check authorisation
        canEdit();

        log.info(LogManager.getHeader(ourContext, "update_collection",
                "collection_id=" + getID()));

        DatabaseManager.update(ourContext, collectionRow);

        if (modified)
        {
            ourContext.addEvent(new Event(Event.MODIFY, Constants.COLLECTION, getID(), null));
            modified = false;
        }
        if (modifiedMetadata)
        {
            ourContext.addEvent(new Event(Event.MODIFY_METADATA, Constants.COLLECTION, getID(), getDetails()));
            modifiedMetadata = false;
            clearDetails();
        }
    }

    public boolean canEditBoolean() throws java.sql.SQLException
    {
        try
        {
            canEdit();

            return true;
        }
        catch (AuthorizeException e)
        {
            return false;
        }
    }

    public void canEdit() throws AuthorizeException, SQLException
    {
        Community[] parents = getCommunities();

        for (int i = 0; i < parents.length; i++)
        {
            if (AuthorizeManager.authorizeActionBoolean(ourContext, parents[i],
                    Constants.WRITE))
            {
                return;
            }

            if (AuthorizeManager.authorizeActionBoolean(ourContext, parents[i],
                    Constants.ADD))
            {
                return;
            }
        }

        AuthorizeManager.authorizeAnyOf(ourContext, this, new int[] {
                Constants.WRITE, Constants.COLLECTION_ADMIN });
    }

    /**
     * Delete the collection, including the metadata and logo. Items that are
     * then orphans are deleted. Groups associated with this collection
     * (workflow participants and submitters) are NOT deleted.
     * 
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    void delete() throws SQLException, AuthorizeException, IOException
    {
        log.info(LogManager.getHeader(ourContext, "delete_collection",
                "collection_id=" + getID()));

        ourContext.addEvent(new Event(Event.DELETE, Constants.COLLECTION, getID(), getHandle()));

        // Remove from cache
        ourContext.removeCached(this, getID());

        // remove subscriptions - hmm, should this be in Subscription.java?
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM subscription WHERE collection_id= ? ", 
                getID());

        // Remove Template Item
        removeTemplateItem();
        
        // Remove items
        ItemIterator items = getAllItems();

        try
        {
        	while (items.hasNext())
        	{
        		Item item = items.next();
        		IndexBrowse ib = new IndexBrowse(ourContext);
        		
        		if (item.isOwningCollection(this))
        		{
        			// the collection to be deletd is the owning collection, thus remove
        			// the item from all collections it belongs to
        			Collection[] collections = item.getCollections();
        			for (int i=0; i< collections.length; i++)
        			{
        				//notify Browse of removing item.
        				ib.itemRemoved(item);
        				// Browse.itemRemoved(ourContext, itemId);
        				collections[i].removeItem(item);
        			}
        			
        		} 
        		// the item was only mapped to this collection, so just remove it
        		else
        		{
        			//notify Browse of removing item mapping. 
        			ib.indexItem(item);
        			// Browse.itemChanged(ourContext, item);
        			removeItem(item);
        		}
        	}
        }
        catch (BrowseException e)
        {
        	log.error("caught exception: ", e);
        	throw new IOException(e.getMessage());
        }

        // Delete bitstream logo
        setLogo(null);

        // Remove all authorization policies
        AuthorizeManager.removeAllPolicies(ourContext, this);

        // Remove any WorkflowItems
        WorkflowItem[] wfarray = WorkflowItem
                .findByCollection(ourContext, this);

        for (int x = 0; x < wfarray.length; x++)
        {
            // remove the workflowitem first, then the item
            Item myItem = wfarray[x].getItem();
            wfarray[x].deleteWrapper();
            myItem.delete();
        }

        // Remove any WorkspaceItems
        WorkspaceItem[] wsarray = WorkspaceItem.findByCollection(ourContext,
                this);

        for (int x = 0; x < wsarray.length; x++)
        {
            wsarray[x].deleteAll();
        }

        // Delete collection row
        DatabaseManager.delete(ourContext, collectionRow);

        // Remove any workflow groups - must happen after deleting collection
        Group g = null;

        g = getWorkflowGroup(1);

        if (g != null)
        {
            g.delete();
        }

        g = getWorkflowGroup(2);

        if (g != null)
        {
            g.delete();
        }

        g = getWorkflowGroup(3);

        if (g != null)
        {
            g.delete();
        }

        // Remove default administrators group
        g = getAdministrators();

        if (g != null)
        {
            g.delete();
        }

        // Remove default submitters group
        g = getSubmitters();

        if (g != null)
        {
            g.delete();
        }
    }

    /**
     * Get the communities this collection appears in
     * 
     * @return array of <code>Community</code> objects
     * @throws SQLException
     */
    public Community[] getCommunities() throws SQLException
    {
        // Get the bundle table rows
        TableRowIterator tri = DatabaseManager.queryTable(ourContext,"community",
                        "SELECT community.* FROM community, community2collection WHERE " +
                        "community.community_id=community2collection.community_id " +
                        "AND community2collection.collection_id= ? ",
                        getID());

        // Build a list of Community objects
        List<Community> communities = new ArrayList<Community>();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // First check the cache
            Community owner = (Community) ourContext.fromCache(Community.class,
                    row.getIntColumn("community_id"));

            if (owner == null)
            {
                owner = new Community(ourContext, row);
            }

            communities.add(owner);

            // now add any parent communities
            Community[] parents = owner.getAllParents();

            for (int i = 0; i < parents.length; i++)
            {
                communities.add(parents[i]);
            }
        }
        // close the TableRowIterator to free up resources
        tri.close();

        Community[] communityArray = new Community[communities.size()];
        communityArray = (Community[]) communities.toArray(communityArray);

        return communityArray;
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Collection
     * as this object, <code>false</code> otherwise
     * 
     * @param other
     *            object to compare to
     * 
     * @return <code>true</code> if object passed in represents the same
     *         collection as this object
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
     * Utility method for reading in a group from a group ID in a column. If the
     * column is null, null is returned.
     * 
     * @param col
     *            the column name to read
     * @return the group referred to by that column, or null
     * @throws SQLException
     */
    private Group groupFromColumn(String col) throws SQLException
    {
        if (collectionRow.isColumnNull(col))
        {
            return null;
        }

        return Group.find(ourContext, collectionRow.getIntColumn(col));
    }

    /**
     * return type found in Constants
     * 
     * @return int Constants.COLLECTION
     */
    public int getType()
    {
        return Constants.COLLECTION;
    }

    /**
     * return an array of collections that user has a given permission on
     * (useful for trimming 'select to collection' list) or figuring out which
     * collections a person is an editor for.
     * 
     * @param context
     * @param comm
     *            (optional) restrict search to a community, else null
     * @param actionID
     *            fo the action
     * 
     * @return Collection [] of collections with matching permissions
     * @throws SQLException
     */
    public static Collection[] findAuthorized(Context context, Community comm,
            int actionID) throws java.sql.SQLException
    {
        List<Collection> myResults = new ArrayList<Collection>();

        Collection[] myCollections = null;

        if (comm != null)
        {
            myCollections = comm.getCollections();
        }
        else
        {
            myCollections = Collection.findAll(context);
        }

        // now build a list of collections you have authorization for
        for (int i = 0; i < myCollections.length; i++)
        {
            if (AuthorizeManager.authorizeActionBoolean(context,
                    myCollections[i], actionID))
            {
                myResults.add(myCollections[i]);
            }
        }

        myCollections = new Collection[myResults.size()];
        myCollections = (Collection[]) myResults.toArray(myCollections);

        return myCollections;
    }

	/**
     * counts items in this collection
     *
     * @return  total items
     */
     public int countItems()
        throws SQLException
     {
        String query = "SELECT count(*) FROM collection2item, item WHERE "
            + "collection2item.collection_id =  ? "
            + "AND collection2item.item_id = item.item_id "
            + "AND in_archive ='1' AND item.withdrawn='0' ";

        PreparedStatement statement = ourContext.getDBConnection().prepareStatement(query);
        statement.setInt(1,getID());
        
        ResultSet rs = statement.executeQuery();
        
        rs.next();
        int itemcount = rs.getInt(1);

        statement.close();

        return itemcount;
     }
}
