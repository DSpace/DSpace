/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
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
