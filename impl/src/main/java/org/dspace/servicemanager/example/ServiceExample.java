/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.example;


/**
 * Sample service.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ServiceExample {

    /**
     * @return some string for testing
     */
    public String getName();

    /**
     * @return some string from another service
     */
    public String getOtherName();

}
