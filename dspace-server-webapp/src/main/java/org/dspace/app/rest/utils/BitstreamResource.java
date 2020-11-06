/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.core.io.AbstractResource;

/**
 * This class acts as a {@link AbstractResource} used by Spring's framework to send the data in a proper and
 * streamlined way inside the {@link org.springframework.http.ResponseEntity} body.
 * This class' attributes are being used by Spring's framework in the overridden methods so that the proper
 * attributes are given and used in the response.
 */
public class BitstreamResource extends AbstractResource {

    private InputStream inputStream;
    private String name;
    private UUID uuid;
    private long sizeBytes;

    public BitstreamResource(InputStream inputStream, String name, UUID uuid, long sizeBytes) {
        this.inputStream = inputStream;
        this.name = name;
        this.uuid = uuid;
        this.sizeBytes = sizeBytes;
    }

    @Override
    public String getDescription() {
        return "bitstream [" + uuid + "]";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public String getFilename() {
        return name;
    }

    @Override
    public long contentLength() throws IOException {
        return sizeBytes;
    }
}
