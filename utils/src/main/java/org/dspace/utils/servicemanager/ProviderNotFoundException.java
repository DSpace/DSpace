/**
 * $Id: ProviderNotFoundException.java 3477 2009-02-16 15:29:24Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/utils/src/main/java/org/dspace/utils/servicemanager/ProviderNotFoundException.java $
 * ProviderNotFoundException.java - DS2 - Feb 16, 2009 2:34:56 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 *
 * 
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
