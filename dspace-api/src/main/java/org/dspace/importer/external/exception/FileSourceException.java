/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.exception;

/**
 * Represents a problem with the File content: e.g. null input stream, invalid content, ...
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */

public class FileSourceException extends Exception {

    private static final long serialVersionUID = 6895579588455260182L;

    public FileSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileSourceException(String message) {
        super(message);
    }
}
