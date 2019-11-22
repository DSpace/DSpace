/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.service;

import org.dspace.scripts.DSpaceRunnable;

/**
 * This service will deal with logic to handle DSpaceRunnable objects
 */
public interface ScriptService {

    /**
     * This method will return the DSpaceRunnable that has the name that's equal to the name given in the parameters
     * @param name  The name that the script has to match
     * @return      The matching DSpaceRunnable script
     */
    DSpaceRunnable getScriptForName(String name);
}
