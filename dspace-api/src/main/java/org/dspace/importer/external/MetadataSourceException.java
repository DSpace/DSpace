/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external;

/** Represents a problem with the input source: e.g. cannot connect to the source.
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class MetadataSourceException extends Exception {
    public MetadataSourceException() {
    }

    public MetadataSourceException(String s) {
        super(s);
    }

    public MetadataSourceException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public MetadataSourceException(Throwable throwable) {
        super(throwable);
    }
}
