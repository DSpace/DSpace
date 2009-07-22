/**
 * $Id: ServiceExample.java 3887 2009-06-18 03:45:35Z mdiggory $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/main/java/org/dspace/servicemanager/example/ServiceExample.java $
 * ServiceExample.java - DSpace2 - Oct 16, 2008 12:29:26 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
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
