/**
 * $Id: ProviderNotFoundException.java 3477 2009-02-16 15:29:24Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/utils/src/main/java/org/dspace/utils/servicemanager/ProviderNotFoundException.java $
 * ProviderNotFoundException.java - DS2 - Feb 16, 2009 2:34:56 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.utils.servicemanager;


/**
 * This exception indicates that the provider was not found (the reference was collected)
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
