/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.supervision.factory;

import org.dspace.supervision.service.SupervisionOrderService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the supervision package,
 * use SupervisionOrderServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public class SupervisionOrderServiceFactoryImpl extends SupervisionOrderServiceFactory {

    @Autowired(required = true)
    private SupervisionOrderService supervisionOrderService;

    @Override
    public SupervisionOrderService getSupervisionOrderService() {
        return supervisionOrderService;
    }
}
