package org.dspace.app.rest.utils;

import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class DSpaceRestInputStreamResource extends InputStreamResource {

    private InputStream inputStream;
    private long contentLength;
    private String filename;

    public DSpaceRestInputStreamResource(InputStream inputStream, Long contentLength, String filename) {
        super(inputStream);
        this.inputStream = inputStream;
        this.contentLength = contentLength;
        this.filename = filename;
    }

    @Override
    public long contentLength() throws IOException {
        return this.contentLength;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public long lastModified() throws IOException {
        return new Date().getTime();
    }

    @Override
    public InputStream getInputStream() throws IOException, IllegalStateException {
        return this.inputStream;
    }




}
