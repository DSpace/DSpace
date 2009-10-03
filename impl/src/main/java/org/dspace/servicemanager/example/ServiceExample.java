/**
 * $Id: ServiceExample.java 3887 2009-06-18 03:45:35Z mdiggory $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/main/java/org/dspace/servicemanager/example/ServiceExample.java $
 * ServiceExample.java - DSpace2 - Oct 16, 2008 12:29:26 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 *
 * 
 */

package org.dspace.servicemanager.example;


/**
 * Sample service
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
