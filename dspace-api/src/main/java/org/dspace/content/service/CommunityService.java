/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.MissingResourceException;

/**
 * Service interface class for the Community object.
 * The implementation of this class is responsible for all business logic calls for the Community object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface CommunityService extends DSpaceObjectService<Community>, DSpaceObjectLegacySupportService<Community>
{


    /**
     * Create a new top-level community, with a new ID.
     *
     * @param parent parent community
     * @param context
     *            DSpace context object
     *
     * @return the newly created community
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Community create(Community parent, Context context) throws SQLException, AuthorizeException;


    /**
     * Create a new top-level community, with a new ID.
     *
     * @param parent parent community
     * @param context
     *            DSpace context object
     * @param handle the pre-determined Handle to assign to the new community
     *
     * @return the newly created community
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Community create(Community parent, Context context, String handle)
            throws SQLException, AuthorizeException;


    /**
     * Get a list of all communities in the system. These are alphabetically
     * sorted by community name.
     *
     * @param context
     *            DSpace context object
     *
     * @return the communities in the system
     * @throws SQLException if database error
     */
    public List<Community> findAll(Context context) throws SQLException;

    /**
     * Get all communities in the system. Adds support for limit and offset.
     * @param context context
     * @param limit limit
     * @param offset offset
     * @return list of communities
     * @throws SQLException if database error
     */
    public List<Community> findAll(Context context, Integer limit, Integer offset) throws SQLException;

    /**
     * Get a list of all top-level communities in the system. These are
     * alphabetically sorted by community name. A top-level community is one
     * without a parent community.
     *
     * @param context
     *            DSpace context object
     *
     * @return the top-level communities in the system
     * @throws SQLException if database error
     */
    public List<Community> findAllTop(Context context) throws SQLException;

    /**
     * Get the value of a metadata field
     *
     * @param community community
     * @param field
     *            the name of the metadata field to get
     *
     * @return the value of the metadata field
     *
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     * @deprecated
     */
    @Override
    @Deprecated
    public String getMetadata(Community community, String field);


    /**
     * Set a metadata value
     *
     * @param context context
     * @param community community
     * @param field
     *            the name of the metadata field to get
     * @param value
     *            value to set the field to
     *
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     * @exception java.util.MissingResourceException
     * @throws SQLException if database error
     * @deprecated
     */
    @Deprecated
    public void setMetadata(Context context, Community community, String field, String value) throws MissingResourceException, SQLException;

    /**
     * Give the community a logo. Passing in <code>null</code> removes any
     * existing logo. You will need to set the format of the new logo bitstream
     * before it will work, for example to "JPEG". Note that
     * <code>update</code> will need to be called for the change to take
     * effect.  Setting a logo and not calling <code>update</code> later may
     * result in a previous logo lying around as an "orphaned" bitstream.
     *
     * @param context context
     * @param community community
     * @param  is   the stream to use as the new logo
     *
     * @return   the new logo bitstream, or <code>null</code> if there is no
     *           logo (<code>null</code> was passed in)
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Bitstream setLogo(Context context, Community community, InputStream is) throws AuthorizeException,
            IOException, SQLException;

    /**
     * Create a default administrators group if one does not already exist.
     * Returns either the newly created group or the previously existing one.
     * Note that other groups may also be administrators.
     *
     * @param context context
     * @param community community
     * @return the default group of editors associated with this community
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Group createAdministrators(Context context, Community community) throws SQLException, AuthorizeException;

    /**
     * Remove the administrators group, if no group has already been created
     * then return without error. This will merely dereference the current
     * administrators group from the community so that it may be deleted
     * without violating database constraints.
     * @param context context
     * @param community community
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public void removeAdministrators(Context context, Community community) throws SQLException, AuthorizeException;

    /**
     * Return an array of parent communities of this community, in ascending
     * order. If community is top-level, return an empty array.
     *
     * @param context context
     * @param community community
     * @return an array of parent communities, empty if top-level
     * @throws SQLException if database error
     */
    public List<Community> getAllParents(Context context, Community community) throws SQLException;

    /**
     * Return an array of parent communities of this collection.
     *
     * @return an array of parent communities
     */
    public List<Community> getAllParents(Context context, Collection collection) throws SQLException;

    /**
     * Return an array of collections of this community and its subcommunities
     *
     * @param context context
     * @param community community
     * @return an array of collections
     * @throws SQLException if database error
     */

    public List<Collection> getAllCollections(Context context, Community community) throws SQLException;


    /**
     * Add an exisiting collection to the community
     *
     * @param context context
     * @param community community
     * @param collection
     *            collection to add
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public void addCollection(Context context, Community community, Collection collection)
            throws SQLException, AuthorizeException;

    /**
     * Create a new sub-community within this community.
     *
     * @param context context
     * @param parentCommunity parent community
     * @return the new community
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Community createSubcommunity(Context context, Community parentCommunity) throws SQLException, AuthorizeException;

    /**
     * Create a new sub-community within this community.
     *
     * @param context context
     * @param handle the pre-determined Handle to assign to the new community
     * @param parentCommunity parent community
     * @return the new community
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Community createSubcommunity(Context context, Community parentCommunity, String handle)
            throws SQLException, AuthorizeException;

    /**
     * Add an existing community as a subcommunity to the community
     *
     * @param context context
     * @param parentCommunity
     *            parent community to add our subcommunity to
     * @param childCommunity
     *            subcommunity to add
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public void addSubcommunity(Context context, Community parentCommunity, Community childCommunity)
            throws SQLException, AuthorizeException;

    /**
     * Remove a collection. If it only belongs to one parent community,
     * then it is permanently deleted. If it has more than one parent community,
     * it is simply unmapped from the current community.
     *
     * @param context context
     * @param c collection to remove
     * @param community community
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     */
    public void removeCollection(Context context, Community community, Collection c)
            throws SQLException, AuthorizeException, IOException;

    /**
     * Remove a subcommunity. If it only belongs to one parent community,
     * then it is permanently deleted. If it has more than one parent community,
     * it is simply unmapped from the current community.
     *
     * @param context context
     * @param childCommunity
     *            subcommunity to remove
     * @param parentCommunity parent community
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     */
    public void removeSubcommunity(Context context, Community parentCommunity, Community childCommunity)
            throws SQLException, AuthorizeException, IOException;

    /**
     * return TRUE if context's user can edit community, false otherwise
     *
     * @param context context
     * @param community community
     * @return boolean true = current user can edit community
     * @throws SQLException if database error
     */
    public boolean canEditBoolean(Context context, Community community) throws java.sql.SQLException;

    public void canEdit(Context context, Community community) throws AuthorizeException, SQLException;

    public Community findByAdminGroup(Context context, Group group) throws SQLException;

    public List<Community> findAuthorized(Context context, List<Integer> actions) throws SQLException;

    public List<Community> findAuthorizedGroupMapped(Context context, List<Integer> actions) throws SQLException;

    int countTotal(Context context) throws SQLException;
}
