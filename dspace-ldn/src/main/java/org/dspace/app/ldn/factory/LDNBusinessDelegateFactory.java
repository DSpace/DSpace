/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.factory;

import org.dspace.app.ldn.LDNBusinessDelegate;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class LDNBusinessDelegateFactory {

    public abstract LDNBusinessDelegate getLDNBusinessDelegate();

    public static LDNBusinessDelegateFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("ldnBusinessDelegateFactory", LDNBusinessDelegateFactory.class);
    }

}
