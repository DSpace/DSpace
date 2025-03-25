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
 * The implementation of this class is responsible for providing metadata to bitstreams that are cloned
 * in order to track which bitstreams are copies, original, replacement, or replaced by.
 *
 * @author Nathan Buckingham at atmire.com
 */
public interface BitstreamLinkingService {

    /**
     * Clones the metadata from an old bitstream to a new one skipping the metadata that we add as a part of
     * the register methods in this service.
     *
     * @param context Dspace Context
     * @param bitstream Dspace bitstream
     * @param clone The bitstream that we are cloning metadata to
     * @throws AuthorizeException If the user inside of context is not authorized to do this
     * @throws SQLException if database error
     */
    void cloneMetadata(Context context, Bitstream bitstream, Bitstream clone) throws SQLException, AuthorizeException;

    /**
     * Registers a new bitstream as a copy for an old version.
     * Adds dspace.bitstream.isCopyOf to newCopy with the UUID of oldCopy
     * Adds dspace.bitstream.hasCopies to oldCopy with the UUID of newCopy
     *
     * @param oldCopy The original bitstream
     * @param newCopy The new version of the bitstream
     * @throws SQLException If database error
     * @throws AuthorizeException If the user inside of context is not authorized to do this
     */
    void registerBitstreams(Context context, Bitstream oldCopy,
                            Bitstream newCopy) throws SQLException, AuthorizeException;

    /**
     * Registers a new bitstream as a replacement for the old version.
     * Adds dspace.bitstream.isReplacementOf to ReplacementCopy with UUID of oldCopy
     * Adds dspace.bitstream.isReplacedBy to oldCopy with UUID of replacementCopy
     *
     * @param context Dspace Context
     * @param oldCopy Dspace bitstream of the old copy
     * @param replacementCopy dspace bitstream that is getting the new metadata
     * @throws SQLException If database error
     * @throws AuthorizeException If the user inside of context is not authorized to do this
     */
    void registerReplacementBitstream(Context context, Bitstream oldCopy,
                                      Bitstream replacementCopy) throws SQLException, AuthorizeException;

    /**
     * Gets the copies of the given bitstream via its dspace.bitstream.hasCopies
     *
     * @param context Dspace Context
     * @param bitstream Dspace bitstream
     * @return List of bitstreams that are copies of the provided bitstream
     * @throws SQLException If database error
     */
    List<Bitstream> getCopies(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Gets the originals of the given bitstream via its dspace.bitstream.isCopyOf
     *
     * @param context Dspace Context
     * @param bitstream Dspace bitstream
     * @return List of bitstreams that are the original of this item. Should only be one
     * @throws SQLException If database error
     */
    List<Bitstream> getOriginals(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Gets the replacements of the given bitstream via its dspace.bitstream.isReplacedBy
     *
     * @param context DSpace Context
     * @param bitstream DSpace bitstream
     * @return List of bitstreams that are the replacement for the provided bitstream should only be one
     * @throws SQLException If database error
     */
    List<Bitstream> getReplacements(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Gets the original replacement of the given bitstream via its dspace.bitstream.isReplacementOf
     *
     * @param context Dspace Context
     * @param bitstream Dspace bitstream
     * @return List of the original bitstream that the provided bitstream replaced should only be one
     * @throws SQLException If database error
     */
    List<Bitstream> getOriginalReplacement(Context context, Bitstream bitstream) throws SQLException;
}
