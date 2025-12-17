/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

/**
 * Exception thrown when a virtual metadata type is invalid or not supported.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class BadVirtualMetadataTypeException extends Exception {

    public BadVirtualMetadataTypeException() {
        super();
    }

    public BadVirtualMetadataTypeException(String s, Throwable t) {
        super(s, t);
    }

    public BadVirtualMetadataTypeException(String s) {
        super(s);
    }

    public BadVirtualMetadataTypeException(Throwable t) {
        super(t);
    }

}