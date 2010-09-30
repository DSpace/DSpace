/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
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
