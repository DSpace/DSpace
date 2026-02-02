/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * When inputstream closes, then delete the file
 * http://stackoverflow.com/a/4694155/368581
 */
public class DeleteOnCloseFileInputStream extends FileInputStream {

    private File file;

    public DeleteOnCloseFileInputStream(String fileName) throws FileNotFoundException {
        this(new File(fileName));
    }

    public DeleteOnCloseFileInputStream(File file) throws FileNotFoundException {
        super(file);
        this.file = file;
    }

    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if (file != null) {
                file.delete();
                file = null;
            }
        }
    }
}
