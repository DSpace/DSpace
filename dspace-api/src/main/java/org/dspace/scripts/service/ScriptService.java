/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.service;

import java.util.List;

import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * This service will deal with logic to handle DSpaceRunnable objects
 */
public interface ScriptService {

    /**
     * This method will return the ScriptConfiguration that has the name that's equal to the name given in the
     * parameters
     * @param name  The name that the script has to match
     * @return The matching ScriptConfiguration
     */
    ScriptConfiguration getScriptConfiguration(String name);

    /**
     * This method will return a list of ScriptConfiguration objects for which the given Context is authorized
     * @param context   The relevant DSpace context
     * @return The list of accessible ScriptConfiguration scripts for this context
     */
    List<ScriptConfiguration> getScriptConfigurations(Context context);

    /**
     * This method will create a new instance of the DSpaceRunnable that's linked with this Scriptconfiguration
     * It'll grab the DSpaceRunnable class from the ScriptConfiguration's variables and create a new instance of it
     * to return
     * @param scriptToExecute   The relevant ScriptConfiguration
     * @return The new instance of the DSpaceRunnable class
     * @throws IllegalAccessException   If something goes wrong
     * @throws InstantiationException   If something goes wrong
     */
    DSpaceRunnable createDSpaceRunnableForScriptConfiguration(ScriptConfiguration scriptToExecute)
        throws IllegalAccessException, InstantiationException;
}
