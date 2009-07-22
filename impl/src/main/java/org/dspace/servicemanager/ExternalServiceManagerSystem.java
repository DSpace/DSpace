package org.dspace.servicemanager;

import org.dspace.servicemanager.config.DSpaceConfigurationService;

import java.util.List;

/**
 * Interface for modular service manager systems.
 * Provides a generic initialization routine, in leiu of hardcoded constructors
 */
public interface ExternalServiceManagerSystem extends ServiceManagerSystem {
    /**
     * Initialize the service manager's configuration
     *
     * @param parent
     * @param configurationService
     * @param testMode
     * @param developmentMode
     * @param serviceManagers
     */
    void init(ServiceManagerSystem parent, DSpaceConfigurationService configurationService,
            boolean testMode, boolean developmentMode, List<ServiceManagerSystem> serviceManagers);

}
