/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.storage.bitstore;

import com.amazonaws.services.s3.model.S3Object;

import java.io.FilterInputStream;
import java.io.IOException;

/**
 * Simple wrapper for S3 InputStreams which properly closes the underlying S3Object when the stream is closed.
 */
public class S3FilterInputStream extends FilterInputStream {

    private S3Object s3Object;

    protected S3FilterInputStream(S3Object object) {
        super(object.getObjectContent());
        s3Object = object;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (s3Object != null) {
            s3Object.close();
        }
    }

}
