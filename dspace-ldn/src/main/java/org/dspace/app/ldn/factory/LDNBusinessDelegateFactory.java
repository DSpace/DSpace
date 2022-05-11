/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.factory;

import org.dspace.app.ldn.service.LDNBusinessDelegate;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract business delegate factory to provide ability to get instance from
 * dspace services factory.
 */
public abstract class LDNBusinessDelegateFactory {

    /**
     * Abstract method to return the business delegate bean.
     *
     * @return LDNBusinessDelegate business delegate bean
     */
    public abstract LDNBusinessDelegate getLDNBusinessDelegate();

    /**
     * Static method to get the business delegate factory instance.
     *
     * @return LDNBusinessDelegateFactory business delegate factory from dspace
     *         services factory
     */
    public static LDNBusinessDelegateFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("ldnBusinessDelegateFactory", LDNBusinessDelegateFactory.class);
    }

}
