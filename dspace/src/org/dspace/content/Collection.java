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
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;


/**
 * Class representing a collection
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

    /** The group of reviewers */
    private Group reviewers;

    /** The group of workflow admins */
    private Group workflowAdmins;

    /** The group of editors */
    private Group editors;

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


        // FIXME: Groups
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

        log.info(LogManager.getHeader(context,
            "create_collection",
            "collection_id=" + row.getIntColumn("collection_id")));

        return new Collection(context, row);
    }


    /**
     * Get all collections in the system.  These are alphabetically
     * sorted by collection name.
     *
     * @param  context  DSpace context object
     *
     * @return  the collections in the system
     */
    public static Collection[] getAllCollections(Context context)
        throws SQLException
    {
        TableRowIterator tri = DatabaseManager.query(context,
            "collection",
            "SELECT * FROM collection ORDER BY name;");

        List collections = new ArrayList();

        while (tri.hasNext())
        {
            TableRow row = tri.next();
            collections.add(new Collection(context, row));
        }

        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);

        return collectionArray;
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
        // FIXME: Check auth

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
     * Set the workflow reviewers
     *
     * @param   g  the group of reviewers
     */
    public void setReviewers(Group g)
    {
        reviewers = g;
    }


    /**
     * Get the workflow reviewers.
     *
     * @return  the group of reviewers
     */
    public Group getReviewers()
    {
        return reviewers;
    }


    /**
     * Set the workflow administrators
     *
     * @param   g  the group of workflow administrators
     */
    public void setWorkflowAdministrators(Group g)
    {
        workflowAdmins = g;
    }


    /**
     * Get the workflow administrators.
     *
     * @return the group of workflow administrators
     */
    public Group getWorkflowAdministrators()
    {
        return workflowAdmins;
    }


    /**
     * Set the workflow editors
     *
     * @param   g  the group of workflow editors
     */
    public void setEditors(Group g)
    {
        editors = g;
    }


    /**
     * Get the workflow editors.
     *
     * @return  the group of workflow editors
     */
    public Group getEditors()
    {
        return editors;
    }


    /**
     * Get the default group of submitters.  Note that the authorization
     * system may allow others to submit to the collection, so this is not
     * necessarily a definitive list of potential submitters.
     * <P>
     * The default group of submitters for collection 100 is the one called
     * <code>collection_100_submit</code>.
     *
     * @return  the default group of submitters.
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

        if (license == null)
        {
            // Fallback to site-wide default
            license = ConfigurationManager.getDefaultSubmissionLicense();
        }

        return license;
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
        // FIXME: Check auth

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
        // FIXME: Check auth

        collectionRow.setColumnNull("template_item_id");
        DatabaseManager.update(ourContext, collectionRow);

        if (template != null)
        {
            log.info(LogManager.getHeader(ourContext,
                "remove_template_item",
                "collection_id=" + getID() +
                    ",template_item_id=" + template.getID()));

            template.deleteWithContents();
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
        // FIXME: Check auth

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
     * Remove an item.  Does not delete the item, just the
     * relationship.  Has instant effect; <code>update</code> need not be
     * called.
     *
     * @param item  item to remove
     */
    public void removeItem(Item item)
        throws SQLException, AuthorizeException
    {
        // FIXME: Check auth

        log.info(LogManager.getHeader(ourContext,
            "remove_item",
            "collection_id=" + getID() + ",item_id=" + item.getID()));

        DatabaseManager.updateQuery(ourContext,
            "DELETE FROM collection2item WHERE collection_id=" + getID() +
            " AND item_id=" + item.getID() + ";");
    }


    /**
     * Update the collection metadata (including logo, and workflow groups)
     * to the database.  Inserts if this is a new collection.
     */
    public void update()
        throws SQLException, AuthorizeException
    {
        // FIXME: Check auth

        log.info(LogManager.getHeader(ourContext,
            "update_collection",
            "collection_id=" + getID()));

        DatabaseManager.update(ourContext, collectionRow);
    }


    /**
     * Delete the collection, including the metadata and logo.  Items
     * are merely disassociated, they are NOT deleted.  If this collection
     * is contained in any communities, the association with those communities
     * is removed.
     */
    public void delete()
        throws SQLException, AuthorizeException, IOException
    {
        // FIXME: Check auth

        log.info(LogManager.getHeader(ourContext,
            "delete_collection",
            "collection_id=" + getID()));

        // Delete collection-item mappings
        DatabaseManager.updateQuery(ourContext,
            "DELETE FROM collection2item WHERE collection_id=" + getID() + ";");

        // Delete bitstream logo
        setLogo(null);

        // Delete collection row
        DatabaseManager.delete(ourContext, collectionRow);

        // FIXME: Groups?
    }


    /**
     * Delete the collection, and recursively the items in the collection.
     * Items or other objects that are multiply contained (e.g. an item also
     * in another collection) are NOT deleted.  If this collection
     * is contained in any communities, the association with those communities
     * is removed.
     */
    public void deleteWithContents()
        throws SQLException, AuthorizeException, IOException
    {
        // FIXME: Check auth

        // Get items
        TableRowIterator items = DatabaseManager.query(ourContext,
            "item",
            "SELECT item.* FROM item, collection2item WHERE " +
                "collection2item.item_id=item.item_id AND " +
                "collection2item.collection_id=" + getID() + ";");


        // Delete collection-item mappings
        DatabaseManager.updateQuery(ourContext,
            "DELETE FROM collection2item WHERE collection_id=" + getID() + ";");

        // Delete community-collection mappings
        DatabaseManager.updateQuery(ourContext,
            "DELETE FROM community2collection WHERE collection_id=" + getID() +
                ";");


        // Delete items if they aren't contained within other collections
        while (items.hasNext())
        {
            Item i = new Item(ourContext, items.next());

            Collection[] collections = i.getCollections();

            if (collections.length == 0)
            {
                // Orphaned item; delete
                i.deleteWithContents();
            }
        }

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
            communities.add(new Community(ourContext, tri.next()));
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
}
