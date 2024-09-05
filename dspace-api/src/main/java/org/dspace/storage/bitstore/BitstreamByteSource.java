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

public class BitstreamByteSource extends ByteSource {

    private static final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    public Bitstream getBitstream() {
        return bitstream;
    }

    private final Bitstream bitstream;

    public BitstreamByteSource(Bitstream bitstream) {
        this.bitstream = bitstream;
    }

    @Override
    public InputStream openStream() throws IOException {
        try {
            return bitstreamService.retrieve(new Context(), bitstream);
        } catch (SQLException | AuthorizeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public long size() throws IOException {
        return bitstream.getSizeBytes();
    }


}
