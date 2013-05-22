/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.utils.servicemanager;


/**
 * This is a special marker interface which is used to indicate that this
 * service is not a provider and thus should not be included in the 
 * provider stacks which are being setup and stored.  Any service which 
 * implements this interface will not be able to be added to the 
 * provider stack.  In some cases this results in an exception but it
 * mostly just results in the object being ignored and not placed into 
 * the stack.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface NotProvider {

    // This area intentionally left blank

}
