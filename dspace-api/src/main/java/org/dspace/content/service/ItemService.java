/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.Thumbnail;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Service interface class for the Item object.
 * The implementation of this class is responsible for all business logic calls for the Item object and is autowired
 * by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ItemService
        extends DSpaceObjectService<Item>, DSpaceObjectLegacySupportService<Item> {

    public Thumbnail getThumbnail(Context context, Item item, boolean requireOriginal) throws SQLException;

    /**
     * Create a new item, with a new internal ID. Authorization is done
     * inside of this method.
     *
     * @param context       DSpace context object
     * @param workspaceItem in progress workspace item
     * @return the newly created item
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public Item create(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException;

    /**
     * Create a new item, with a provided ID. Authorisation is done
     * inside of this method.
     *
     * @param context DSpace context object
     * @param workspaceItem in progress workspace item
     * @param uuid the pre-determined UUID to assign to the new item
     * @return the newly created item
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Item create(Context context, WorkspaceItem workspaceItem, UUID uuid) throws SQLException, AuthorizeException;

    /**
     * Create an empty template item for this collection. If one already exists,
     * no action is taken. Caution: Make sure you call <code>update</code> on
     * the collection after doing this, or the item will have been created but
     * the collection record will not refer to it.
     *
     * @param context    DSpace context object
     * @param collection Collection (parent)
     * @return empty template item for this collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public Item createTemplateItem(Context context, Collection collection) throws SQLException, AuthorizeException;

    /**
     * Get all the items in the archive. Only items with the "in archive" flag
     * set are included. The order of the list is indeterminate.
     *
     * @param context DSpace context object
     * @return an iterator over the items in the archive.
     * @throws SQLException if database error
     */
    public Iterator<Item> findAll(Context context) throws SQLException;

    /**
     * Get all the items in the archive. Only items with the "in archive" flag
     * set are included. The order of the list is indeterminate.
     *
     * @param context DSpace context object
     * @param limit   limit
     * @param offset  offset
     * @return an iterator over the items in the archive.
     * @throws SQLException if database error
     */
    public Iterator<Item> findAll(Context context, Integer limit, Integer offset) throws SQLException;

    /**
     * Get all "final" items in the archive, both archived ("in archive" flag) or
     * withdrawn items are included. The order of the list is indeterminate.
     *
     * @param context DSpace context object
     * @return an iterator over the items in the archive.
     * @throws SQLException if database error
     */
    @Deprecated
    public Iterator<Item> findAllUnfiltered(Context context) throws SQLException;

    /**
     * Find all items that are:
     * - NOT in the workspace
     * - NOT in the workflow
     * - NOT a template item for e.g. a collection
     *
     * This implies that the result also contains older versions of items and withdrawn items.
     * @param context the DSpace context.
     * @return iterator over all regular items.
     * @throws SQLException if database error.
     */
    public Iterator<Item> findAllRegularItems(Context context) throws SQLException;

    /**
     * Find all the items in the archive by a given submitter. The order is
     * indeterminate. Only items with the "in archive" flag set are included.
     *
     * @param context DSpace context object
     * @param eperson the submitter
     * @return an iterator over the items submitted by eperson
     * @throws SQLException if database error
     */
    public Iterator<Item> findBySubmitter(Context context, EPerson eperson)
        throws SQLException;

    /**
     * Find all the items by a given submitter. The order is
     * indeterminate. All items are included.
     *
     * @param context DSpace context object
     * @param eperson the submitter
     * @param retrieveAllItems flag to determine if all items should be returned or only archived items.
     *                         If true, all items (regardless of status) are returned.
     *                         If false, only archived items will be returned.
     * @return an iterator over the items submitted by eperson
     * @throws SQLException if database error
     */
    public Iterator<Item> findBySubmitter(Context context, EPerson eperson, boolean retrieveAllItems)
            throws SQLException;

    /**
     * Retrieve the list of items submitted by eperson, ordered by recently submitted, optionally limitable
     *
     * @param context DSpace context object
     * @param eperson the submitter
     * @param limit   a positive integer to limit, -1 or null for unlimited
     * @return an iterator over the items submitted by eperson
     * @throws SQLException if database error
     */
    public Iterator<Item> findBySubmitterDateSorted(Context context, EPerson eperson, Integer limit)
        throws SQLException;

    /**
     * Get all the archived items in this collection. The order is indeterminate.
     *
     * @param context    DSpace context object
     * @param collection Collection (parent)
     * @return an iterator over the items in the collection.
     * @throws SQLException if database error
     */
    public Iterator<Item> findByCollection(Context context, Collection collection) throws SQLException;

    /**
     * Get all the archived items in this collection. The order is indeterminate.
     *
     * @param context    DSpace context object
     * @param collection Collection (parent)
     * @param limit      limited number of items
     * @param offset     offset value
     * @return an iterator over the items in the collection.
     * @throws SQLException if database error
     */
    public Iterator<Item> findByCollection(Context context, Collection collection, Integer limit, Integer offset)
        throws SQLException;

    /**
     * Get all the archived items mapped to this collection (excludes owning collection). The order is indeterminate.
     *
     * @param context    DSpace context object
     * @param collection Collection (parent)
     * @param limit      limited number of items
     * @param offset     offset value
     * @return an iterator over the items in the collection.
     * @throws SQLException if database error
     */
    public Iterator<Item> findByCollectionMapping(Context context, Collection collection, Integer limit, Integer offset)
            throws SQLException;

    /**
     * Count all the archived items mapped to this collection (excludes owning collection). The order is indeterminate.
     *
     * @param context    DSpace context object
     * @param collection Collection (parent)
     * @return an iterator over the items in the collection.
     * @throws SQLException if database error
     */
    public int countByCollectionMapping(Context context, Collection collection) throws SQLException;

    /**
     * Get all the items (including private and withdrawn) in this collection. The order is indeterminate.
     *
     * @param context DSpace context object
     * @param collection Collection (parent)
     * @return an iterator over the items in the collection.
     * @param limit limited number of items
     * @param offset offset value
     * @throws SQLException if database error
     */
    public Iterator<Item> findAllByCollection(Context context, Collection collection, Integer limit, Integer offset)
        throws SQLException;

    /**
     * Get all Items installed or withdrawn, discoverable, and modified since a Date.
     *
     * @param context DSpace context object
     * @param since   earliest interesting last-modified date, or null for no date test.
     * @return an iterator over the items in the collection.
     * @throws SQLException if database error
     */
    public Iterator<Item> findInArchiveOrWithdrawnDiscoverableModifiedSince(Context context, Date since)
        throws SQLException;

    /**
     * Get all Items installed or withdrawn, NON-discoverable, and modified since a Date.
     * @param context context
     * @param since earliest interesting last-modified date, or null for no date test.
     * @return an iterator over the items in the collection.
     * @throws SQLException if database error
     */
    public Iterator<Item> findInArchiveOrWithdrawnNonDiscoverableModifiedSince(Context context, Date since)
        throws SQLException;

    /**
     * Get all the items (including private and withdrawn) in this collection. The order is indeterminate.
     *
     * @param context    DSpace context object
     * @param collection Collection (parent)
     * @return an iterator over the items in the collection.
     * @throws SQLException if database error
     */
    public Iterator<Item> findAllByCollection(Context context, Collection collection) throws SQLException;

    /**
     * See whether this Item is contained by a given Collection.
     *
     * @param item       item to check
     * @param collection Collection (parent
     * @return true if {@code collection} contains this Item.
     * @throws SQLException if database error
     */
    public boolean isIn(Item item, Collection collection) throws SQLException;

    /**
     * Get the communities this item is in. Returns an unordered array of the
     * communities that house the collections this item is in, including parent
     * communities of the owning collections.
     *
     * @param context DSpace context object
     * @param item    item to check
     * @return the communities this item is in.
     * @throws SQLException if database error
     */
    public List<Community> getCommunities(Context context, Item item) throws SQLException;


    /**
     * Get the bundles matching a bundle name (name corresponds roughly to type)
     *
     * @param item item to check
     * @param name name of bundle (ORIGINAL/TEXT/THUMBNAIL)
     * @return the bundles in an unordered array
     * @throws SQLException if database error
     */
    public List<Bundle> getBundles(Item item, String name) throws SQLException;

    /**
     * Add an existing bundle to this item. This has immediate effect.
     *
     * @param context DSpace context object
     * @param item    item to add the bundle to
     * @param bundle  the bundle to add
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void addBundle(Context context, Item item, Bundle bundle) throws SQLException, AuthorizeException;

    /**
     * Remove a bundle. This may result in the bundle being deleted, if the
     * bundle is orphaned.
     *
     * @param context DSpace context object
     * @param item    Item
     * @param bundle  the bundle to remove
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    public void removeBundle(Context context, Item item, Bundle bundle) throws SQLException, AuthorizeException,
        IOException;

    /**
     * Remove all bundles linked to this item. This may result in the bundle being deleted, if the
     * bundle is orphaned.
     *
     * @param context DSpace context object
     * @param item    the item from which to remove all bundles
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    public void removeAllBundles(Context context, Item item) throws AuthorizeException, SQLException, IOException;

    /**
     * Create a single bitstream in a new bundle. Provided as a convenience
     * method for the most common use.
     *
     * @param context DSpace context object
     * @param item    item to create bitstream on
     * @param is      the stream to create the new bitstream from
     * @param name    is the name of the bundle (ORIGINAL, TEXT, THUMBNAIL)
     * @return Bitstream that is created
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     * @throws SQLException       if database error
     */
    public Bitstream createSingleBitstream(Context context, InputStream is, Item item, String name)
        throws AuthorizeException, IOException, SQLException;

    /**
     * Convenience method, calls createSingleBitstream() with name "ORIGINAL"
     *
     * @param context DSpace context object
     * @param item    item to create bitstream on
     * @param is      InputStream
     * @return created bitstream
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     * @throws SQLException       if database error
     */
    public Bitstream createSingleBitstream(Context context, InputStream is, Item item)
        throws AuthorizeException, IOException, SQLException;

    /**
     * Get all non-internal bitstreams in the item. This is mainly used for
     * auditing for provenance messages and adding format.* DC values. The order
     * is indeterminate.
     *
     * @param context DSpace context object
     * @param item    item to check
     * @return non-internal bitstreams.
     * @throws SQLException if database error
     */
    public List<Bitstream> getNonInternalBitstreams(Context context, Item item) throws SQLException;

    /**
     * Remove just the DSpace license from an item This is useful to update the
     * current DSpace license, in case the user must accept the DSpace license
     * again (either the item was rejected, or resumed after saving)
     * <p>
     * This method is used by the org.dspace.submit.step.LicenseStep class
     *
     * @param context DSpace context object
     * @param item    item to remove DSpace license from
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    public void removeDSpaceLicense(Context context, Item item) throws SQLException, AuthorizeException,
        IOException;

    /**
     * Remove all licenses from an item - it was rejected
     *
     * @param context DSpace context object
     * @param item    item to remove all licenses from
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    public void removeLicenses(Context context, Item item) throws SQLException, AuthorizeException, IOException;

    /**
     * Withdraw the item from the archive. It is kept in place, and the content
     * and metadata are not deleted, but it is not publicly accessible.
     *
     * @param context DSpace context object
     * @param item    item to withdraw
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void withdraw(Context context, Item item) throws SQLException, AuthorizeException;


    /**
     * Reinstate a withdrawn item
     *
     * @param context DSpace context object
     * @param item    withdrawn item to reinstate
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void reinstate(Context context, Item item) throws SQLException, AuthorizeException;

    /**
     * Return true if this Collection 'owns' this item
     *
     * @param item       item to check
     * @param collection Collection
     * @return true if this Collection owns this item
     */
    public boolean isOwningCollection(Item item, Collection collection);

    /**
     * remove all of the policies for item and replace them with a new list of
     * policies
     *
     * @param context     DSpace context object
     * @param item        item to replace policies on
     * @param newpolicies -
     *                    this will be all of the new policies for the item and its
     *                    contents
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void replaceAllItemPolicies(Context context, Item item, List<ResourcePolicy> newpolicies)
        throws SQLException,
        AuthorizeException;

    /**
     * remove all of the policies for item's bitstreams and bundles and replace
     * them with a new list of policies
     *
     * @param context     DSpace context object
     * @param item        item to replace policies on
     * @param newpolicies -
     *                    this will be all of the new policies for the bundle and
     *                    bitstream contents
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void replaceAllBitstreamPolicies(Context context, Item item, List<ResourcePolicy> newpolicies)
        throws SQLException, AuthorizeException;


    /**
     * remove all of the policies for item's bitstreams and bundles that belong
     * to a given Group
     *
     * @param context DSpace context object
     * @param item    item to remove group policies from
     * @param group   Group referenced by policies that needs to be removed
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void removeGroupPolicies(Context context, Item item, Group group) throws SQLException, AuthorizeException;

    /**
     * remove all policies on an item and its contents, and replace them with
     * the DEFAULT_ITEM_READ and DEFAULT_BITSTREAM_READ policies belonging to
     * the collection.
     *
     * @param context    DSpace context object
     * @param item       item to reset policies on
     * @param collection Collection
     * @throws SQLException       if database error
     *                            if an SQL error or if no default policies found. It's a bit
     *                            draconian, but default policies must be enforced.
     * @throws AuthorizeException if authorization error
     */
    public void inheritCollectionDefaultPolicies(Context context, Item item, Collection collection)
        throws java.sql.SQLException, AuthorizeException;

    /**
     * Adjust the Bundle and Bitstream policies to reflect what have been defined
     * during the submission/workflow. The temporary SUBMISSION and WORKFLOW
     * policies are removed and the policies defined at the item and collection
     * level are copied and inherited as appropriate. Custom selected Item policies
     * are copied to the bundle/bitstream only if no explicit custom policies were
     * already applied to the bundle/bitstream. Collection's policies are inherited
     * if there are no other policies defined or if the append mode is defined by
     * the configuration via the core.authorization.installitem.inheritance-read.append-mode property
     * 
     * @param context             DSpace context object
     * @param item                Item to adjust policies on
     * @param collection          Collection
     * @throws SQLException       If database error
     * @throws AuthorizeException If authorization error
     */
    public void adjustBundleBitstreamPolicies(Context context, Item item, Collection collection)
        throws SQLException, AuthorizeException;


    /**
     * Adjust the Item's policies to reflect what have been defined during the
     * submission/workflow. The temporary SUBMISSION and WORKFLOW policies are
     * removed and the default policies defined at the collection level are
     * inherited as appropriate. Collection's policies are inherited if there are no
     * other policies defined or if the append mode is defined by the configuration
     * via the core.authorization.installitem.inheritance-read.append-mode property
     * 
     * @param context              DSpace context object
     * @param item                 Item to adjust policies on
     * @param collection           Collection
     * @throws SQLException        If database error
     * @throws AuthorizeException  If authorization error
     */
    public void adjustItemPolicies(Context context, Item item, Collection collection)
        throws SQLException, AuthorizeException;

    /**
     * Moves the item from one collection to another one
     *
     * @param context DSpace context object
     * @param item    item to move
     * @param from    Collection to move from
     * @param to      Collection to move to
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    public void move(Context context, Item item, Collection from, Collection to)
        throws SQLException, AuthorizeException, IOException;

    /**
     * Moves the item from one collection to another one
     *
     * @param context                DSpace context object
     * @param item                   item to move
     * @param from                   Collection to move from
     * @param to                     Collection to move to
     * @param inheritDefaultPolicies whether to inherit policies from new collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    public void move(Context context, Item item, Collection from, Collection to, boolean inheritDefaultPolicies)
        throws SQLException, AuthorizeException, IOException;

    /**
     * Check the bundle ORIGINAL to see if there are any uploaded files
     *
     * @param item item to check
     * @return true if there is a bundle named ORIGINAL with one or more
     * bitstreams inside
     * @throws SQLException if database error
     */
    public boolean hasUploadedFiles(Item item) throws SQLException;

    /**
     * Get the collections this item is not in.
     *
     * @param context DSpace context object
     * @param item    item to check
     * @return the collections this item is not in, if any.
     * @throws SQLException if database error
     */
    public List<Collection> getCollectionsNotLinked(Context context, Item item) throws SQLException;

    /**
     * return TRUE if context's user can edit item, false otherwise
     *
     * @param context DSpace context object
     * @param item    item to check
     * @return boolean true = current user can edit item
     * @throws SQLException if database error
     */
    public boolean canEdit(Context context, Item item) throws java.sql.SQLException;

    /**
     * return TRUE if context's user can create new version of the item, false
     * otherwise.
     *
     * @param context DSpace context object
     * @param item    item to check
     * @return boolean true = current user can create new version of the item
     * @throws SQLException if database error
     */
    public boolean canCreateNewVersion(Context context, Item item) throws SQLException;

    /**
     * Returns an iterator of in archive items possessing the passed metadata field, or only
     * those matching the passed value, if value is not Item.ANY
     *
     * @param context   DSpace context object
     * @param schema    metadata field schema
     * @param element   metadata field element
     * @param qualifier metadata field qualifier
     * @param value     field value or Item.ANY to match any value
     * @return an iterator over the items matching that authority value
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public Iterator<Item> findArchivedByMetadataField(Context context, String schema,
                                                      String element, String qualifier,
                                                      String value) throws SQLException, AuthorizeException;

    /**
     * Returns an iterator of in archive items possessing the passed metadata field, or only
     * those matching the passed value, if value is not Item.ANY
     *
     * @param context   DSpace context object
     * @param metadataField    metadata
     * @param value     field value or Item.ANY to match any value
     * @return an iterator over the items matching that authority value
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public Iterator<Item> findArchivedByMetadataField(Context context, String metadataField, String value)
            throws SQLException, AuthorizeException;

    /**
     * Returns an iterator of Items possessing the passed metadata field, or only
     * those matching the passed value, if value is not Item.ANY
     *
     * @param context   DSpace context object
     * @param schema    metadata field schema
     * @param element   metadata field element
     * @param qualifier metadata field qualifier
     * @param value     field value or Item.ANY to match any value
     * @return an iterator over the items matching that authority value
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    public Iterator<Item> findByMetadataField(Context context,
                                              String schema, String element, String qualifier, String value)
        throws SQLException, AuthorizeException, IOException;

    public Iterator<Item> findByMetadataQuery(Context context, List<List<MetadataField>> listFieldList,
                                              List<String> query_op, List<String> query_val, List<UUID> collectionUuids,
                                              String regexClause, int offset, int limit)
        throws SQLException, AuthorizeException, IOException;

    /**
     * Find all the items in the archive with a given authority key value
     * in the indicated metadata field.
     *
     * @param context   DSpace context object
     * @param schema    metadata field schema
     * @param element   metadata field element
     * @param qualifier metadata field qualifier
     * @param value     the value of authority key to look for
     * @return an iterator over the items matching that authority value
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    public Iterator<Item> findByAuthorityValue(Context context,
                                               String schema, String element, String qualifier, String value)
        throws SQLException, AuthorizeException;


    public Iterator<Item> findByMetadataFieldAuthority(Context context, String mdString, String authority)
        throws SQLException, AuthorizeException;

    /**
     * Service method for knowing if this Item should be visible in the item list.
     * Items only show up in the "item list" if the user has READ permission
     * and if the Item isn't flagged as unlisted.
     *
     * @param context DSpace context object
     * @param item    item
     * @return true or false
     */
    public boolean isItemListedForUser(Context context, Item item);

    /**
     * counts items in the given collection
     *
     * @param context    DSpace context object
     * @param collection Collection
     * @return total items
     * @throws SQLException if database error
     */
    public int countItems(Context context, Collection collection) throws SQLException;

    /**
     * counts all items in the given collection including withdrawn items
     *
     * @param context DSpace context object
     * @param collection Collection
     * @return total items
     * @throws SQLException if database error
     */
    public int countAllItems(Context context, Collection collection) throws SQLException;

    /**
     * Find all Items modified since a Date.
     *
     * @param context DSpace context object
     * @param last    Earliest interesting last-modified date.
     * @return iterator over items
     * @throws SQLException if database error
     */
    public Iterator<Item> findByLastModifiedSince(Context context, Date last)
        throws SQLException;

    /**
     * counts items in the given community
     *
     * @param context   DSpace context object
     * @param community Community
     * @return total items
     * @throws SQLException if database error
     */
    public int countItems(Context context, Community community) throws SQLException;

    /**
     * counts all items in the given community including withdrawn
     *
     * @param context DSpace context object
     * @param community Community
     * @return total items
     * @throws SQLException if database error
     */
    public int countAllItems(Context context, Community community) throws SQLException;

    /**
     * counts all items
     *
     * @param context DSpace context object
     * @return total items
     * @throws SQLException if database error
     */
    int countTotal(Context context) throws SQLException;

    /**
     * counts all items not in archive
     *
     * @param context DSpace context object
     * @return total items NOT in archive
     * @throws SQLException if database error
     */
    int countNotArchivedItems(Context context) throws SQLException;

    /**
     * counts all items in archive
     *
     * @param context DSpace context object
     * @return total items in archive
     * @throws SQLException if database error
     */
    int countArchivedItems(Context context) throws SQLException;

    /**
     * counts all withdrawn items
     *
     * @param context DSpace context object
     * @return total items withdrawn
     * @throws SQLException if database error
     */
    int countWithdrawnItems(Context context) throws SQLException;

    /**
     * Check if the supplied item is an inprogress submission
     *
     * @param context DSpace context object
     * @param item    item to check
     * @return <code>true</code> if the item is linked to a workspaceitem or workflowitem
     * @throws SQLException if database error
     */
    boolean isInProgressSubmission(Context context, Item item) throws SQLException;

    /**
     * Get metadata for the DSpace Object in a chosen schema.
     * See <code>MetadataSchema</code> for more information about schemas.
     * Passing in a <code>null</code> value for <code>qualifier</code>
     * or <code>lang</code> only matches metadata fields where that
     * qualifier or languages is actually <code>null</code>.
     * Passing in <code>DSpaceObject.ANY</code>
     * retrieves all metadata fields with any value for the qualifier or
     * language, including <code>null</code>
     * <P>
     * Examples:
     * <P>
     * Return values of the unqualified "title" field, in any language.
     * Qualified title fields (e.g. "title.uniform") are NOT returned:
     * <P>
     * <code>dspaceobject.getMetadataByMetadataString("dc", "title", null, DSpaceObject.ANY );</code>
     * <P>
     * Return all US English values of the "title" element, with any qualifier
     * (including unqualified):
     * <P>
     * <code>dspaceobject.getMetadataByMetadataString("dc, "title", DSpaceObject.ANY, "en_US" );</code>
     * <P>
     * The ordering of values of a particular element/qualifier/language
     * combination is significant. When retrieving with wildcards, values of a
     * particular element/qualifier/language combinations will be adjacent, but
     * the overall ordering of the combinations is indeterminate.
     *
     * If enableVirtualMetadata is set to false, the virtual metadata will not be included
     *
     * @param item         Item
     * @param schema       the schema for the metadata field. <em>Must</em> match
     *                     the <code>name</code> of an existing metadata schema.
     * @param element      the element name. <code>DSpaceObject.ANY</code> matches any
     *                     element. <code>null</code> doesn't really make sense as all
     *                     metadata must have an element.
     * @param qualifier    the qualifier. <code>null</code> means unqualified, and
     *                     <code>DSpaceObject.ANY</code> means any qualifier (including
     *                     unqualified.)
     * @param lang         the ISO639 language code, optionally followed by an underscore
     *                     and the ISO3166 country code. <code>null</code> means only
     *                     values with no language are returned, and
     *                     <code>DSpaceObject.ANY</code> means values with any country code or
     *                     no country code are returned.
     * @param enableVirtualMetadata
     *                     Enables virtual metadata calculation and inclusion from the
     *                     relationships.
     * @return metadata fields that match the parameters
     */
    public List<MetadataValue> getMetadata(Item item, String schema, String element, String qualifier,
                                           String lang, boolean enableVirtualMetadata);

    /**
     * Retrieve the label of the entity type of the given item.
     * @param item the item.
     * @return the label of the entity type, taken from the item metadata, or null if not found.
     */
    public String getEntityTypeLabel(Item item);

    /**
     * Retrieve the entity type of the given item.
     * @param context the DSpace context.
     * @param item the item.
     * @return the entity type of the given item, or null if not found.
     */
    public EntityType getEntityType(Context context, Item item) throws SQLException;

}
