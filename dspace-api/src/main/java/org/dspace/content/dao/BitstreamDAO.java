/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Database Access Object interface class for the Bitstream object.
 * The implementation of this class is responsible for all database calls for the Bitstream object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface BitstreamDAO extends DSpaceObjectLegacySupportDAO<Bitstream> {

    public List<Bitstream> findDeletedBitstreams(Context context) throws SQLException;

    public List<Bitstream> findDuplicateInternalIdentifier(Context context, Bitstream bitstream) throws SQLException;

    public List<Bitstream> findBitstreamsWithNoRecentChecksum(Context context) throws SQLException;

    public Iterator<Bitstream> findByCommunity(Context context, Community community) throws SQLException;

    public Iterator<Bitstream> findByCollection(Context context, Collection collection) throws SQLException;

    public Iterator<Bitstream> findByItem(Context context, Item item) throws SQLException;

    public Iterator<Bitstream> findByStoreNumber(Context context, Integer storeNumber) throws SQLException;

    public Long countByStoreNumber(Context context, Integer storeNumber) throws SQLException;

    int countRows(Context context) throws SQLException;

    int countDeleted(Context context) throws SQLException;

    int countWithNoPolicy(Context context) throws SQLException;

    List<Bitstream> getNotReferencedBitstreams(Context context) throws SQLException;
}
