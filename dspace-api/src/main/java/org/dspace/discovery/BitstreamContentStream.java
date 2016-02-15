/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.log4j.Logger;
import org.apache.solr.common.util.ContentStreamBase;
import org.dspace.content.Bitstream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Construct a <code>ContentStream</code> from a <code>File</code>
 */
public class BitstreamContentStream extends ContentStreamBase
{
    private static final Logger log = Logger.getLogger(BitstreamContentStream.class);
    private final Bitstream file;

    public BitstreamContentStream( Bitstream f ) {
        file = f;

        contentType = f.getFormat().getMIMEType();
        name = file.getName();
        size = file.getSize();
        sourceInfo = file.getName();
    }

    @Override
    public String getContentType() {
        if(contentType==null) {
            InputStream stream = null;
            try {
                stream = file.retrieve();
                char first = (char)stream.read();
                if(first == '<') {
                    return "application/xml";
                }
                if(first == '{') {
                    return "application/json";
                }
            } catch(Exception ex) {
                log.error("Error determining content type for bitstream:" + file.getID(), ex);
            } finally {
                if (stream != null) try {
                    stream.close();
                } catch (IOException ioe) {
                    log.error("Error closing stream:" + file.getID(), ioe);

                }
            }
        }
        return contentType;
    }

    @Override
    public InputStream getStream() throws IOException {
        try {
            return file.retrieve();
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return new ByteArrayInputStream(e.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }
}

