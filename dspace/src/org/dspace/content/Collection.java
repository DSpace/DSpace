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
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;


// NOTES/ISSUES:
//         Transactionally allocate IDs?  I can see a concurrency problem
//         Throw AuthorizeException at create() or just update()?
//         Create empty groups for reviewers, editors, wfadmins?


/**
 * Class representing a collection
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class Collection
{
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
        return null;
    }
    
    
    /**
     * Create a new collection, with a new ID.  Not inserted in database.
     *
     * @param  context  DSpace context object
     *
     * @return  the newly created collection
     */
    public static Collection create(Context context)
        throws AuthorizeException
    {
        return null;
    }

    
    /**
     * Get a list of all collections in the system
     *
     * @param  context  DSpace context object
     *
     * @return  the collections in the system
     */
    public static List getAllCollections(Context context)
        throws SQLException
    {
        return null;
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
        return null;
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
    }


    /**
     * Get the logo for the collection.  <code>null</code> is return if the
     * collection does not have a logo.
     *
     * @return the logo of the collection, or <code>null</code>
     */
    public Bitstream getLogo()
    {
        return null;
    }

    
    /**
     * Give the collection a logo.  
     *
     * @param is  stream of a JPEG logo, or <code>null</code> for no logo
     */
    public void setLogo(InputStream is)
        throws AuthorizeException, IOException
    {
    }
    
    
    /**
     * Set the workflow reviewers
     *
     * @param   g  the group of reviewers
     */
    public void setReviewers(Group g)
    {
    }


    /**
     * Get the workflow reviewers.
     *
     * @param   g  Get the group of reviewers
     */
    public Group getReviewers()
    {
        return null;
    }


    /**
     * Set the workflow administrators
     *
     * @param   g  the group of workflow administrators
     */
    public void setWorkflowAdministrators(Group g)
    {
    }


    /**
     * Get the workflow administrators.
     *
     * @param   g  Get the group of workflow administrators
     */
    public Group getWorkflowAdministrators()
    {
        return null;
    }


    /**
     * Set the workflow editors
     *
     * @param   g  the group of workflow editors
     */
    public void setEditors(Group g)
    {
    }


    /**
     * Get the workflow editors.
     *
     * @param   g  Get the group of workflow editors
     */
    public Group getEditors()
    {
        return null;
    }


    /**
     * Get the default group of submitters.  Note that the authorization
     * system may allow others to submit to the collection, so this is not
     * necessarily a definitive list of potential submitters.
     * <P>
     * The default group of submitters for collection 100 is the one called
     * <code>collection_100_submit</code>.
     *
     * @param   g  Get the default group of submitters.
     */
    public Group getSubmitters()
    {
        return null;
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
        return null;
    }


    /**
     * Set the license for this collection.  Passing in <code>null</code>
     * means that the site-wide default will be used.
     *
     * @param  license  the license, or <code>null</code>
     */
    public void setLicense(String license)
    {
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
        return null;
    }
    
    
    /**
     * Create an empty template item for this collection.  If one already
     * exists, no action is taken.
     */
    public void createTemplateItem()
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Remove the template item for this collection, if there is one
     */
    public void removeTemplateItem()
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Add an item to the collection.  This simply adds a relationship between
     * the item and the collection - it does nothing like set an issue date,
     * remove a personal workspace item etc.
     *
     * @param item  item to add
     */
    public void addItem(Item item)
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Remove an item.  Does not delete the item, just the
     * relationship.
     *
     * @param item  item to remove
     */
    public void removeItem(Item item)
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Update the collection metadata (including logo, and workflow groups)
     * to the database.  Inserts if this is a new collection.
     */
    public void update()
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Delete the collection, including the metadata and logo.  Items
     * are merely disassociated, they are NOT deleted.  If this collection
     * is contained in any communities, the association with those communities
     * is removed.
     */
    public void delete()
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Delete the collection, and recursively the items in the collection.
     * Items or other objects that are multiply contained (e.g. an item also
     * in another collection) are NOT deleted.  If this collection
     * is contained in any communities, the association with those communities
     * is removed.
     */
    public void deleteWithContents()
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Get recent additions to the collection.
     *
     * @param  n  the number of recent additions to retrieve.
     *
     * @return  a list of the most recent additions.
     */
    public List getRecentAdditions(int n)
        throws SQLException
    {
        return null;
    }


    /**
     * Get the communities this collection appears in
     *
     * @return   a <code>List</code> of <code>Community</code> objects
     */
    public List getCommunities()
        throws SQLException
    {
        return null;
    }
}
