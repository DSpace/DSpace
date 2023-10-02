/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the Bitstream object.
 * The implementation of this class is responsible for all database calls for the Bitstream object and is autowired
 * by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface BitstreamDAO extends DSpaceObjectLegacySupportDAO<Bitstream> {

    public Iterator<Bitstream> findAll(Session session, int limit, int offset) throws SQLException;

    public List<Bitstream> findDeletedBitstreams(Session session, int limit, int offset) throws SQLException;

    public List<Bitstream> findDuplicateInternalIdentifier(Session session, Bitstream bitstream) throws SQLException;

    public List<Bitstream> findBitstreamsWithNoRecentChecksum(Session session) throws SQLException;

    public Iterator<Bitstream> findByCommunity(Session session, Community community) throws SQLException;

    public Iterator<Bitstream> findByCollection(Session session, Collection collection) throws SQLException;

    public Iterator<Bitstream> findByItem(Session session, Item item) throws SQLException;

    public Iterator<Bitstream> findByStoreNumber(Session session, Integer storeNumber) throws SQLException;

    public Long countByStoreNumber(Session session, Integer storeNumber) throws SQLException;

    int countRows(Session session) throws SQLException;

    int countDeleted(Session session) throws SQLException;

    int countWithNoPolicy(Session session) throws SQLException;

    List<Bitstream> getNotReferencedBitstreams(Session session) throws SQLException;
}
