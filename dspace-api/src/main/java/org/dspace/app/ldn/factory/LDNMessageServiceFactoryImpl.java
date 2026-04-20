/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.factory;

import org.dspace.app.ldn.service.LDNMessageService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the notifyservices package, use
 * NotifyServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.com)
 */
public class LDNMessageServiceFactoryImpl extends LDNMessageServiceFactory {

    @Autowired(required = true)
    private LDNMessageService ldnMessageService;

    @Override
    public LDNMessageService getLDNMessageService() {
        return ldnMessageService;
    }

}
