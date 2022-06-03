/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

public class BigMultipartFile implements MultipartFile {

    private final String name;
    private String originalFilename;
    @Nullable
    private String contentType;
    private InputStream inputStream;

    public BigMultipartFile(String name, String originalFilename, @Nullable String contentType,
                            InputStream inputStream) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.inputStream = inputStream;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getOriginalFilename() {
        return this.originalFilename;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public boolean isEmpty() {
        return ObjectUtils.isEmpty(this.inputStream);
    }

    @Override
    public long getSize() {
        try {
            throw new IOException("This implementation doesn't support bytes[].");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public byte[] getBytes() throws IOException {
        throw new IOException("This implementation doesn't support bytes[].");
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.inputStream;
    }

    @Override
    public void transferTo(File file) throws IOException, IllegalStateException {
        throw new IOException("Cannot transfer file to the bytes[]" +
                " because this implementation doesn't support bytes[].");
    }
}
