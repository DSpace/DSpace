/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.factory;

import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the NotifyService package,
 * use NotifyServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.com)
 */
public abstract class LDNMessageServiceFactory {

    public abstract LDNMessageService getLDNMessageService();

    public static LDNMessageServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance()
                .getServiceManager()
                .getServiceByName("ldnMessageServiceFactory",
                    LDNMessageServiceFactory.class);
    }
}
