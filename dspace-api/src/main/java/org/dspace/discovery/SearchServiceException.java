/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

/**
 * Exception used by discovery when discovery search exceptions occur
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SearchServiceException extends Exception {

    public SearchServiceException() {
    }

    public SearchServiceException(String s) {
        super(s);
    }

    public SearchServiceException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public SearchServiceException(Throwable throwable) {
        super(throwable);
    }
    
}
