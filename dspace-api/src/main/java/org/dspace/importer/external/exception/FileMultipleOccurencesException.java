/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.exception;

/**
 * This exception could be throws when more than one element is found
 * in a method that works on one only.
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */

public class FileMultipleOccurencesException extends Exception {

    private static final long serialVersionUID = 1222409723339501937L;

    public FileMultipleOccurencesException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileMultipleOccurencesException(String message) {
        super(message);
    }
}
