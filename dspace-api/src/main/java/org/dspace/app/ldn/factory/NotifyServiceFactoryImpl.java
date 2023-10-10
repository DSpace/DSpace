/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.factory;

import org.dspace.app.ldn.service.NotifyPatternToTriggerService;
import org.dspace.app.ldn.service.NotifyService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the notifyservices package,
 * use NotifyServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceFactoryImpl extends NotifyServiceFactory {

    @Autowired(required = true)
    private NotifyService notifyService;

    @Autowired(required = true)
    private NotifyPatternToTriggerService notifyPatternToTriggerService;

    @Override
    public NotifyService getNotifyService() {
        return notifyService;
    }

    @Override
    public NotifyPatternToTriggerService getNotifyPatternToTriggerService() {
        return notifyPatternToTriggerService;
    }

}
