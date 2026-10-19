/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.factory;

import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.app.ldn.service.NotifyPatternToTriggerService;
import org.dspace.app.ldn.service.NotifyService;
import org.dspace.app.ldn.service.NotifyServiceInboundPatternService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the NotifyService package,
 * use NotifyServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public abstract class NotifyServiceFactory {

    public abstract NotifyService getNotifyService();

    public abstract NotifyServiceInboundPatternService getNotifyServiceInboundPatternService();

    public abstract NotifyPatternToTriggerService getNotifyPatternToTriggerService();

    public abstract LDNMessageService getLDNMessageService();

    public static NotifyServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
            "notifyServiceFactory", NotifyServiceFactory.class);
    }
}
