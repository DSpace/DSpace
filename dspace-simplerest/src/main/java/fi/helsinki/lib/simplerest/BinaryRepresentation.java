/**
 A RESTful web service on top of DSpace.
 The contents of this file are subject to the license and copyright
 detailed in the LICENSE and NOTICE files at the root of the source
 tree and available online at
 http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import org.restlet.representation.OutputRepresentation;
import org.restlet.data.MediaType;

import org.apache.commons.io.IOUtils;

public class BinaryRepresentation extends OutputRepresentation {

    private InputStream inputStream;

    public BinaryRepresentation(MediaType mediaType, InputStream inputStream) {
        super(mediaType);
        this.inputStream = inputStream;
    }
    
    public void write(OutputStream outputStream) throws IOException {
        IOUtils.copy(this.inputStream, outputStream);
    }
}