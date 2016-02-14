/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.factory;

import org.dspace.eperson.service.*;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the eperson package, use EPersonServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class EPersonServiceFactory {

    public abstract EPersonService getEPersonService();

    public abstract GroupService getGroupService();

    public abstract RegistrationDataService getRegistrationDataService();

    public abstract AccountService getAccountService();

    public abstract SubscribeService getSubscribeService();

    public abstract SupervisorService getSupervisorService();

    public static EPersonServiceFactory getInstance(){
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("ePersonServiceFactory", EPersonServiceFactory.class);
    }
}
