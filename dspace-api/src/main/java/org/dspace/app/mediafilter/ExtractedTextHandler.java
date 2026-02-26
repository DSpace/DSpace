/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.InputStream;

/**
 * Callback class to handle text extracted by a {@link FormatFilter}.
 *
 * @author mwood
 */
public interface ExtractedTextHandler {
    /**
     * Callback to absorb the content of a stream.
     *
     * @param stream extracted text.
     * @throws Exception passed through.
     */
    public void handleStream(InputStream stream) throws Exception;
}
