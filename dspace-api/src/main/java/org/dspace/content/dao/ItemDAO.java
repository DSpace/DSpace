/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;

/**
 * Database Access Object interface class for the Item object.
 * The implementation of this class is responsible for all database calls for the Item object and is autowired by spring
 * This class should only be accessed from a single service & should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ItemDAO extends DSpaceObjectLegacySupportDAO<Item>
{
    public Iterator<Item> findAll(Context context, boolean archived) throws SQLException;

    public Iterator<Item> findAll(Context context, boolean archived, boolean withdrawn) throws SQLException;

    /**
     * Find all Items modified since a Date.
     *
     * @param context
     * @param since Earliest interesting last-modified date.
     * @return 
     */
    public Iterator<Item> findByLastModifiedSince(Context context, Date since)
            throws SQLException;

    public Iterator<Item> findBySubmitter(Context context, EPerson eperson) throws SQLException;

    public Iterator<Item> findBySubmitter(Context context, EPerson eperson, MetadataField metadataField, int limit) throws SQLException;

    public Iterator<Item> findByMetadataField(Context context, MetadataField metadataField, String value, boolean inArchive) throws SQLException;

    public Iterator<Item> findByAuthorityValue(Context context, MetadataField metadataField, String authority, boolean inArchive) throws SQLException;

    public Iterator<Item> findArchivedByCollection(Context context, Collection collection, Integer limit, Integer offset) throws SQLException;

    public Iterator<Item> findAllByCollection(Context context, Collection collection) throws SQLException;

    public int countItems(Context context, Collection collection, boolean includeArchived, boolean includeWithdrawn) throws SQLException;

    /**
     * Get all Items installed or withdrawn, discoverable, and modified since a Date.
     * @param context
     * @param archived
     * @param withdrawn
     * @param discoverable
     * @param lastModified earliest interesting last-modified date.
     * @return
     */
    public Iterator<Item> findAll(Context context, boolean archived,
            boolean withdrawn, boolean discoverable, Date lastModified)
            throws SQLException;
}
