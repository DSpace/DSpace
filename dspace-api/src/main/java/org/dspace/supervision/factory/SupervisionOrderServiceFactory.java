/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.supervision.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.supervision.service.SupervisionOrderService;

/**
 * Abstract factory to get services for the supervision package,
 * use SupervisionOrderServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public abstract class SupervisionOrderServiceFactory {

    public abstract SupervisionOrderService getSupervisionOrderService();

    public static SupervisionOrderServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance()
                                    .getServiceManager()
                                    .getServiceByName("supervisionOrderServiceFactory",
                                        SupervisionOrderServiceFactory.class);
    }
}
