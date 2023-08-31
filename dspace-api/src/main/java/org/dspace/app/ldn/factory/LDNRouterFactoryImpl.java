/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.factory;

import org.dspace.app.ldn.LDNRouter;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Factory implementation to get services for the notifyservices package,
 * use NotifyServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class LDNRouterFactoryImpl extends LDNRouterFactory {

    @Autowired(required = true)
    private LDNRouter ldnRouter;

    @Override
    public LDNRouter getLDNRouter() {
        return ldnRouter;
    }

}
