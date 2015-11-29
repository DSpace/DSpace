/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Service interface class for the Item object.
 * The implementation of this class is responsible for all business logic calls for the Item object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ItemService extends DSpaceObjectService<Item>, DSpaceObjectLegacySupportService<Item>
{
    public Thumbnail getThumbnail(Context context, Item item, boolean requireOriginal) throws SQLException;

    /**
     * Create a new item, with a new internal ID. This method is not public,
     * since items need to be created as workspace items. Authorisation is the
     * responsibility of the caller.
     *
     * @param context
     *            DSpace context object
     * @return the newly created item
     * @throws SQLException
     * @throws AuthorizeException
     */
    public Item create(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException;

    /**
     * Create an empty template item for this collection. If one already exists,
     * no action is taken. Caution: Make sure you call <code>update</code> on
     * the collection after doing this, or the item will have been created but
     * the collection record will not refer to it.
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    public Item createTemplateItem(Context context, Collection collection) throws SQLException, AuthorizeException;

    /**
     * Get all the items in the archive. Only items with the "in archive" flag
     * set are included. The order of the list is indeterminate.
     *
     * @param context
     *            DSpace context object
     * @return an iterator over the items in the archive.
     * @throws SQLException
     */
    public Iterator<Item> findAll(Context context) throws SQLException;

    /**
     * Get all "final" items in the archive, both archived ("in archive" flag) or
     * withdrawn items are included. The order of the list is indeterminate.
     *
     * @param context
     *            DSpace context object
     * @return an iterator over the items in the archive.
     * @throws SQLException
     */
    public Iterator<Item> findAllUnfiltered(Context context) throws SQLException;

    /**
     * Find all the items in the archive by a given submitter. The order is
     * indeterminate. Only items with the "in archive" flag set are included.
     *
     * @param context
     *            DSpace context object
     * @param eperson
     *            the submitter
     * @return an iterator over the items submitted by eperson
     * @throws SQLException
     */
    public Iterator<Item> findBySubmitter(Context context, EPerson eperson)
            throws SQLException;

    /**
     * Retrieve the list of Items submitted by eperson, ordered by recently submitted, optionally limitable
     * @param context
     * @param eperson
     * @param limit a positive integer to limit, -1 or null for unlimited
     * @return
     * @throws SQLException
     */
    public Iterator<Item> findBySubmitterDateSorted(Context context, EPerson eperson, Integer limit) throws SQLException;

    /**
     * Get all the items in this collection. The order is indeterminate.
     *
     * @return an iterator over the items in the collection.
     * @throws SQLException
     */
    public Iterator<Item> findByCollection(Context context, Collection collection) throws SQLException;

    /**
     * Get all the items in this collection. The order is indeterminate.
     *
     * @return an iterator over the items in the collection.
     * @throws SQLException
     */
    public Iterator<Item> findByCollection(Context context, Collection collection, Integer limit, Integer offset) throws SQLException;

    /**
     * Get all Items installed or withdrawn, discoverable, and modified since a Date.
     * @param context
     * @param since earliest interesting last-modified date, or null for no date test.
     * @return 
     */
    public Iterator<Item> findInArchiveOrWithdrawnDiscoverableModifiedSince(Context context, Date since)
            throws SQLException;

    /**
     * Get all the items in this collection. The order is indeterminate.
     *
     * @return an iterator over the items in the collection.
     * @throws SQLException
     */
    public Iterator<Item> findAllByCollection(Context context, Collection collection) throws SQLException;

    /**
     * See whether this Item is contained by a given Collection.
     * @param collection
     * @return true if {@code collection} contains this Item.
     * @throws SQLException
     */
    public boolean isIn(Item item, Collection collection) throws SQLException;

    /**
     * Get the communities this item is in. Returns an unordered array of the
     * communities that house the collections this item is in, including parent
     * communities of the owning collections.
     *
     * @return the communities this item is in.
     * @throws SQLException
     */
    public List<Community> getCommunities(Context context, Item item) throws SQLException;


    /**
     * Get the bundles matching a bundle name (name corresponds roughly to type)
     *
     * @param name
     *            name of bundle (ORIGINAL/TEXT/THUMBNAIL)
     *
     * @return the bundles in an unordered array
     */
    public List<Bundle> getBundles(Item item, String name) throws SQLException;

    /**
     * Add an existing bundle to this item. This has immediate effect.
     *
     * @param bundle
     *            the bundle to add
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void addBundle(Context context, Item item, Bundle bundle) throws SQLException, AuthorizeException;

    /**
     * Remove a bundle. This may result in the bundle being deleted, if the
     * bundle is orphaned.
     *
     * @param bundle
     *            the bundle to remove
     * @throws SQLException
     * @throws AuthorizeException
     * @throws java.io.IOException
     */
    public void removeBundle(Context context, Item item, Bundle bundle) throws SQLException, AuthorizeException,
            IOException;

    /**
     * Remove all bundles linked to this item. This may result in the bundle being deleted, if the
     * bundle is orphaned.
     *
     * @param item
     *            the item from which to remove our bundles
     * @throws SQLException
     * @throws AuthorizeException
     * @throws java.io.IOException
     */
    public void removeAllBundles(Context context, Item item) throws AuthorizeException, SQLException, IOException;

    /**
     * Create a single bitstream in a new bundle. Provided as a convenience
     * method for the most common use.
     *
     * @param is
     *            the stream to create the new bitstream from
     * @param name
     *            is the name of the bundle (ORIGINAL, TEXT, THUMBNAIL)
     * @return Bitstream that is created
     * @throws AuthorizeException
     * @throws IOException
     * @throws SQLException
     */
    public Bitstream createSingleBitstream(Context context, InputStream is, Item item, String name)
            throws AuthorizeException, IOException, SQLException;

    /**
     * Convenience method, calls createSingleBitstream() with name "ORIGINAL"
     *
     * @param is
     *            InputStream
     * @return created bitstream
     * @throws AuthorizeException
     * @throws IOException
     * @throws SQLException
     */
    public Bitstream createSingleBitstream(Context context, InputStream is, Item item)
            throws AuthorizeException, IOException, SQLException;

    /**
     * Get all non-internal bitstreams in the item. This is mainly used for
     * auditing for provenance messages and adding format.* DC values. The order
     * is indeterminate.
     *
     * @return non-internal bitstreams.
     */
    public List<Bitstream> getNonInternalBitstreams(Context context, Item item) throws SQLException;

    /**
     * Remove just the DSpace license from an item This is useful to update the
     * current DSpace license, in case the user must accept the DSpace license
     * again (either the item was rejected, or resumed after saving)
     * <p>
     * This method is used by the org.dspace.submit.step.LicenseStep class
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeDSpaceLicense(Context context, Item item) throws SQLException, AuthorizeException,
            IOException;

    /**
     * Remove all licenses from an item - it was rejected
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeLicenses(Context context, Item item) throws SQLException, AuthorizeException, IOException;

    /**
     * Withdraw the item from the archive. It is kept in place, and the content
     * and metadata are not deleted, but it is not publicly accessible.
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void withdraw(Context context, Item item) throws SQLException, AuthorizeException;


    /**
     * Reinstate a withdrawn item
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void reinstate(Context context, Item item) throws SQLException, AuthorizeException;

    /**
     * Return true if this Collection 'owns' this item
     *
     * @param collection
     *            Collection
     * @return true if this Collection owns this item
     */
    public boolean isOwningCollection(Item item, Collection collection);

    /**
     * remove all of the policies for item and replace them with a new list of
     * policies
     *
     * @param newpolicies -
     *            this will be all of the new policies for the item and its
     *            contents
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void replaceAllItemPolicies(Context context, Item item, List<ResourcePolicy> newpolicies) throws SQLException,
            AuthorizeException;

    /**
     * remove all of the policies for item's bitstreams and bundles and replace
     * them with a new list of policies
     *
     * @param newpolicies -
     *            this will be all of the new policies for the bundle and
     *            bitstream contents
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void replaceAllBitstreamPolicies(Context context, Item item, List<ResourcePolicy> newpolicies)
            throws SQLException, AuthorizeException;


    /**
     * remove all of the policies for item's bitstreams and bundles that belong
     * to a given Group
     *
     * @param group
     *            Group referenced by policies that needs to be removed
     * @throws SQLException
     */
    public void removeGroupPolicies(Context context, Item item, Group group) throws SQLException, AuthorizeException;

    /**
     * remove all policies on an item and its contents, and replace them with
     * the DEFAULT_ITEM_READ and DEFAULT_BITSTREAM_READ policies belonging to
     * the collection.
     *
     * @param collection
     *            Collection
     * @throws java.sql.SQLException
     *             if an SQL error or if no default policies found. It's a bit
     *             draconian, but default policies must be enforced.
     * @throws AuthorizeException
     */
    public void inheritCollectionDefaultPolicies(Context context, Item item, Collection collection)
            throws java.sql.SQLException, AuthorizeException;

    public void adjustBundleBitstreamPolicies(Context context, Item item, Collection collection) throws SQLException, AuthorizeException;


    public void adjustItemPolicies(Context context, Item item, Collection collection) throws SQLException, AuthorizeException;

    /**
     * Moves the item from one collection to another one
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void move(Context context, Item item, Collection from, Collection to) throws SQLException, AuthorizeException, IOException;

    /**
     * Moves the item from one collection to another one
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void move (Context context, Item item, Collection from, Collection to, boolean inheritDefaultPolicies) throws SQLException, AuthorizeException, IOException;

    /**
     * Check the bundle ORIGINAL to see if there are any uploaded files
     *
     * @return true if there is a bundle named ORIGINAL with one or more
     *         bitstreams inside
     * @throws SQLException
     */
    public boolean hasUploadedFiles(Item item) throws SQLException;

    /**
     * Get the collections this item is not in.
     *
     * @return the collections this item is not in, if any.
     * @throws SQLException
     */
    public List<Collection> getCollectionsNotLinked(Context context, Item item) throws SQLException;

    /**
     * return TRUE if context's user can edit item, false otherwise
     *
     * @return boolean true = current user can edit item
     * @throws SQLException
     */
    public boolean canEdit(Context context, Item item) throws java.sql.SQLException;

    /**
     * Returns an iterator of Items possessing the passed metadata field, or only
     * those matching the passed value, if value is not Item.ANY
     *
     * @param context DSpace context object
     * @param schema metadata field schema
     * @param element metadata field element
     * @param qualifier metadata field qualifier
     * @param value field value or Item.ANY to match any value
     * @return an iterator over the items matching that authority value
     * @throws SQLException, AuthorizeException, IOException
     *
     */
    public Iterator<Item> findByMetadataField(Context context,
                                              String schema, String element, String qualifier, String value)
            throws SQLException, AuthorizeException, IOException;


    /**
     * Find all the items in the archive with a given authority key value
     * in the indicated metadata field.
     *
     * @param context DSpace context object
     * @param schema metadata field schema
     * @param element metadata field element
     * @param qualifier metadata field qualifier
     * @param value the value of authority key to look for
     * @return an iterator over the items matching that authority value
     * @throws SQLException, AuthorizeException, IOException
     */
    public Iterator<Item> findByAuthorityValue(Context context,
                                               String schema, String element, String qualifier, String value)
            throws SQLException, AuthorizeException, IOException;


    public Iterator<Item> findByMetadataFieldAuthority(Context context, String mdString, String authority) throws SQLException, AuthorizeException;

    /**
     * Service method for knowing if this Item should be visible in the item list.
     * Items only show up in the "item list" if the user has READ permission
     * and if the Item isn't flagged as unlisted.
     * @param context
     * @param item
     * @return
     */
    public boolean isItemListedForUser(Context context, Item item);

    /**
     * counts items in the given collection
     *
     * @return  total items
     */
    public int countItems(Context context, Collection collection) throws SQLException;

    /**
     * Find all Items modified since a Date.
     *
     * @param context
     * @param last Earliest interesting last-modified date.
     * @return 
     */
    public Iterator<Item> findByLastModifiedSince(Context context, Date last)
            throws SQLException;

/**
     * counts items in the given community
     *
     * @return total items
     */
    public int countItems(Context context, Community community) throws SQLException;
}
