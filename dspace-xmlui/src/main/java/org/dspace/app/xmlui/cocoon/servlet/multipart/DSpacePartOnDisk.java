/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon.servlet.multipart;

import java.io.*;
import java.util.*;
import org.apache.cocoon.servlet.multipart.*;

/**
 * This class represents a file part parsed from a http post stream.
 *
 * @version $Id: PartOnDisk.java 587750 2007-10-24 02:35:22Z vgritsenko $
 */
public class DSpacePartOnDisk extends Part {

    /** Field file */
    private File file = null;
    private int size;

    /**
     * Constructor PartOnDisk
     *
     * @param headers
     * @param file
     */
    public DSpacePartOnDisk(Map headers, File file) {
        super(headers);
        this.file = file;

        // Ensure the file will be deleted when we exit the JVM
        this.file.deleteOnExit();

        this.size = file.length()>new Long(Integer.MAX_VALUE)?Integer.MAX_VALUE:((int) file.length());
    }

    /**
     * Returns the file name
     */
    public String getFileName() {
        return file.getName();
    }

    /**
     * Returns the file size in bytes
     */
    public int getSize() {
        return this.size;
    }

    /**
     * Returns the file
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns a (ByteArray)InputStream containing the file data
     *
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        if (this.file != null) {
            return new FileInputStream(file);
        }
        throw new IllegalStateException("This part has already been disposed.");
    }

    /**
     * Returns the filename
     */
    public String toString() {
        return file.getPath();
    }

    /**
     * Delete the underlying file.
     */
    public void dispose() {
        if (this.file != null) {
            this.file.delete();
            this.file = null;
        }
    }

    /**
     * Ensures the underlying file has been deleted
     */
    public void finalize() throws Throwable {
        // Ensure the file has been deleted
        dispose();
        super.finalize();
    }
}