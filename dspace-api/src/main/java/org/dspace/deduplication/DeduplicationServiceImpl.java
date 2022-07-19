/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deduplication;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.deduplication.dao.DeduplicationDAO;
import org.dspace.deduplication.service.DeduplicationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * Deduplication service implementation
 *
 * @author 4Science
 */
public class DeduplicationServiceImpl implements DeduplicationService {

    /**
     * log4j logger
     */
    private final Logger log = LogManager.getLogger(DeduplicationServiceImpl.class);

    @Autowired(required = true)
    protected DeduplicationDAO deduplicationDAO;

    protected DeduplicationServiceImpl() {
        super();
    }

    /**
     * Create a new Deduplication object
     *
     * @param context The relevant DSpace Context.
     * @return the created Deduplication object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    @Override
    public Deduplication create(Context context, Deduplication d) throws SQLException {
        Deduplication dedup = deduplicationDAO.create(context, d);
        return dedup;
    }

    /**
     * Return all deduplication objects
     *
     * @param context
     * @param pageSize
     * @param offset
     * @return The list al all deduplication objects
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    @Override
    public List<Deduplication> findAll(Context context, int pageSize, int offset) throws SQLException {
        return deduplicationDAO.findAll(context, pageSize, offset);
    }

    /**
     * Count all accounts.
     *
     * @param context The relevant DSpace Context.
     * @return the total number of deduplication
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    @Override
    public int countTotal(Context context) throws SQLException {
        return deduplicationDAO.countRows(context);
    }

    /**
     * Save a Deduplication object
     *
     * @param context The relevant DSpace Context.
     * @param dedup   The deduplication object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    @Override
    public void update(Context context, Deduplication dedup) throws SQLException {
        deduplicationDAO.save(context, dedup);
    }

    /**
     * Get deduplication entries where the item IDs match the given first and second item IDs
     * @param context   DSpace context
     * @param firstId   first item ID
     * @param secondId  second item ID
     * @return          List of deduplication objects
     * @throws SQLException
     */
    @Override
    public List<Deduplication> getDeduplicationByFirstAndSecond(Context context, UUID firstId, UUID secondId)
            throws SQLException {
        return deduplicationDAO.findByFirstAndSecond(context, firstId, secondId);
    }

    /**
     * Get a single, unique deduplication entry where the item IDs match the given first and second item IDs
     * @param context   DSpace context
     * @param firstId   first item ID
     * @param secondId  second item ID
     * @return          Deduplication objects
     * @throws SQLException
     */
    @Override
    public Deduplication uniqueDeduplicationByFirstAndSecond(Context context, UUID firstId, UUID secondId)
            throws SQLException {
        return deduplicationDAO.uniqueByFirstAndSecond(context, firstId, secondId);
    }
}
