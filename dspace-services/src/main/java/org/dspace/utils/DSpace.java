/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.utils;

import org.dspace.kernel.DSpaceKernel;
import org.dspace.kernel.DSpaceKernelManager;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.ConfigurationService;
import org.dspace.services.EventService;
import org.dspace.services.RequestService;
import org.dspace.services.SessionService;


/**
 * This is the DSpace helper services access object.
 * It allows access to all core DSpace services for those who cannot or 
 * will not use the injection service to get services.
 * <p>
 * Note that this may not include every core service but should include 
 * all the services that are useful to UI developers at least.
 * <p>
 * This should be initialized using the constructor and then can be used 
 * as long as the kernel is not shutdown.  Making multiple copies of
 * this is cheap and can be done without worry about the cost.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class DSpace {

    private DSpaceKernel kernel;
    public DSpaceKernel getKernel() {
        return kernel;
    }

    /**
     * Construct a DSpace helper object which uses the default kernel.
     *
     * @throws IllegalStateException if the kernel is not already running
     */
    public DSpace() {
        this(null);
    }

    /**
     * Construct a DSpace helper object which uses the a specific named 
     * instance of the kernel.
     *
     * @param kernelName the name of the kernel to use (null to use the default kernel)
     * @throws IllegalStateException if the kernel is not already running or no kernel exists with this name
     */
    public DSpace(String kernelName) {
        DSpaceKernel kernel = new DSpaceKernelManager().getKernel(kernelName);
        this.kernel = kernel;
    }

    public ServiceManager getServiceManager() {
        if (kernel == null) {
            throw new IllegalStateException("DSpace kernel cannot be null");
        }
        return kernel.getServiceManager();
    }

    // place methods to retrieve key services below here -AZ

    public ConfigurationService getConfigurationService() {
        return getServiceManager().getServiceByName(ConfigurationService.class.getName(), ConfigurationService.class);
    }

    public EventService getEventService() {
        return getServiceManager().getServiceByName(EventService.class.getName(), EventService.class);
    }
    
    public SessionService getSessionService() {
        return getServiceManager().getServiceByName(SessionService.class.getName(), SessionService.class);
    }

    public RequestService getRequestService() {
        return getServiceManager().getServiceByName(RequestService.class.getName(), RequestService.class);
    }
    
    public <T> T getSingletonService(Class<T> type) {
        return getServiceManager().getServiceByName(type.getName(), type);
    }
    
}
