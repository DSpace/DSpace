/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.utils.servicemanager;


/**
 * This exception indicates that the provider was not found (the 
 * reference was collected).
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ProviderNotFoundException extends RuntimeException {

    public ProviderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderNotFoundException(String message) {
        super(message);
    }
    
}
