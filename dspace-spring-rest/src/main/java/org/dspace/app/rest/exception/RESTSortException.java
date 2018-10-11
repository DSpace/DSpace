/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import org.dspace.sort.SortException;

/**
 * Wrapper of the dspace SortException that makes it unchecked.
 *
 * @author Michael Spalti
 */
public class RESTSortException extends RuntimeException {

    public RESTSortException(String message) {
        super(message);
    }

    public RESTSortException(SortException ex) { super(ex); }

    public RESTSortException(String message, Throwable ex) {
        super(message, ex);
    }

}