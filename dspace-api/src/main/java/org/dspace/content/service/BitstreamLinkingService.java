/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;

/**
 * Service interface class for BitstreamLinking
 * The implementation of this class is responsible for providing links between bitstreams via the versioning service
 *
 * @author Nathan Buckingham at atmire.com
 */
public interface BitstreamLinkingService {

    /**
     * Registers a new bitstream as a copy for an old version.
     * Adds dspace.bitstream.isCopyOf to newCopy with oldCopiesID
     * Adds dspace.bitstream.hasCopies to oldCopy with  newCopiesID
     *
     * @param oldCopy The original bitstream
     * @param newCopy The new version of the bitstream
     */
    void registerBitstreams(Context context, Bitstream oldCopy,
                            Bitstream newCopy) throws SQLException, AuthorizeException;

    /**
     * Registers a new bitstream as a replacement for the old version.
     * Adds dspace.bitstream.isReplacementOf to ReplacementCopy with oldCopiesID
     * Adds dspace.bitstream.isReplacedBy to oldCopy with ReplacementCopiesID
     *
     * @param context
     * @param oldCopy
     * @param replacementCopy
     * @throws SQLException
     */
    void registerReplacementBitstream(Context context, Bitstream oldCopy,
                                      Bitstream replacementCopy) throws SQLException, AuthorizeException;

    /**
     * Gets the copies of the given bitstream via its dspace.bitstream.hasCopies
     *
     * @param context
     * @param bitstream
     * @return
     * @throws SQLException
     */
    List<Bitstream> getCopies(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Gets the originals of the given bitstream via its dspace.bitstream.isCopyOf
     *
     * @param context
     * @param bitstream
     * @return
     * @throws SQLException
     */
    List<Bitstream> getOriginals(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Gets the replacements of the given bitstream via its dspace.bitstream.isReplacedBy
     *
     * @param context
     * @param bitstream
     * @return
     * @throws SQLException
     */
    List<Bitstream> getReplacements(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Gets the original replacement of the given bitstream via its dspace.bitstream.isReplacementOf
     *
     * @param context
     * @param bitstream
     * @return
     * @throws SQLException
     */
    List<Bitstream> getOriginalReplacement(Context context, Bitstream bitstream) throws SQLException;
}
