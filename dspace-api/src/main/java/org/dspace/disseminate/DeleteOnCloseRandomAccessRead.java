/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;

/**
 * A RandomAccessReadBufferedFile wrapper that deletes the backing file when closed.
 */
class DeleteOnCloseRandomAccessRead extends RandomAccessReadBufferedFile {

    private final Path path;

    DeleteOnCloseRandomAccessRead(Path path) throws IOException {
        super(path);
        this.path = path;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            Files.deleteIfExists(path);
        }
    }
}
