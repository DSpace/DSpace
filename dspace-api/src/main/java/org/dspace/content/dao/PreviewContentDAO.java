/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.PreviewContent;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the PreviewContent object.
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author Michaela Paurikova (dspace at dataquest.sk)
 */
public interface PreviewContentDAO extends GenericDAO<PreviewContent> {
    /**
     * Find all preview content based on ID of bitstream the preview content is added to.
     *
     * @param context       DSpace context
     * @param bitstreamId   The bitstream ID
     * @return              List of found preview content
     * @throws SQLException If a database error occurs
     */
    List<PreviewContent> findByBitstream(Context context, UUID bitstreamId) throws SQLException;

    /**
     *  Find all preview content based on bitstream that are the root directory.
     *
     * @param context       DSpace context
     * @param bitstreamId   The bitstream ID
     * @return              List of found preview content
     * @throws SQLException If a database error occurs
     */
    List<PreviewContent> findRootByBitstream(Context context, UUID bitstreamId) throws SQLException;
}
