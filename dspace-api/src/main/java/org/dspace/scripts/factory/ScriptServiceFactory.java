/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.factory;

import org.dspace.scripts.service.ProcessService;
import org.dspace.scripts.service.ScriptService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the Script workload, use ScriptServiceFactory.getInstance() to retrieve an
 * implementation
 *
 */
public abstract class ScriptServiceFactory {

    /**
     * This method will return an instance of the ScriptService
     * @return An instance of the ScriptService
     */
    public abstract ScriptService getScriptService();

    /**
     * This method will return an instance of the ProcessService
     * @return  An instance of the ProcessService
     */
    public abstract ProcessService getProcessService();

    /**
     * Use this method to retrieve an implementation of the ScriptServiceFactory to use to retrieve the different beans
     * @return An implementation of the ScriptServiceFactory
     */
    public static ScriptServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("scriptServiceFactory", ScriptServiceFactory.class);
    }
}
