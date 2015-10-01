/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services;

/**
 * Interface whos implementations will be called when the kernel startup is completed.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface KernelStartupCallbackService {

    public void executeCallback();
}
