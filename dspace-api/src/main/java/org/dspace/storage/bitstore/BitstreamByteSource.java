/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import com.google.common.io.ByteSource;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;

/**
 * A ByteSource implementation that provides access to DSpace Bitstream content.
 * Extends Google Guava's ByteSource to allow streaming access to bitstream data.
 *
 * Author: Mark Diggory, Nathan Buckingham
 */
public class BitstreamByteSource extends ByteSource {

    /** Service for accessing bitstream content */
    private static final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    /** The bitstream this source provides access to */
    private final Bitstream bitstream;

    /**
     * Creates a new BitstreamByteSource for the given bitstream.
     *
     * @param bitstream the DSpace bitstream to wrap
     */
    public BitstreamByteSource(Bitstream bitstream) {
        this.bitstream = bitstream;
    }

    /**
     * Gets the underlying bitstream.
     *
     * @return the DSpace bitstream object
     */
    public Bitstream getBitstream() {
        return bitstream;
    }

    /**
     * Opens a new input stream for reading the bitstream content.
     *
     * @return an input stream containing the bitstream data
     * @throws IOException if there is an error retrieving the bitstream,
     *         including SQL or authorization errors
     */
    @Override
    public InputStream openStream() throws IOException {
        try {
            return bitstreamService.retrieve(new Context(), bitstream);
        } catch (SQLException | AuthorizeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Gets the size of the bitstream in bytes.
     *
     * @return the size of the bitstream in bytes
     * @throws IOException if there is an error accessing the size
     */
    @Override
    public long size() throws IOException {
        return bitstream.getSizeBytes();
    }


}
