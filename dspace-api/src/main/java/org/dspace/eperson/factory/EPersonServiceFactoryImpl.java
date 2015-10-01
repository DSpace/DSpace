/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.factory;

import org.dspace.eperson.service.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the eperson package, use EPersonServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class EPersonServiceFactoryImpl extends EPersonServiceFactory {

    @Autowired(required = true)
    private GroupService groupService;
    @Autowired(required = true)
    private EPersonService epersonService;
    @Autowired(required = true)
    private RegistrationDataService registrationDataService;
    @Autowired(required = true)
    private AccountService accountService;
    @Autowired(required = true)
    private SubscribeService subscribeService;
    @Autowired(required = true)
    private SupervisorService supervisorService;

    @Override
    public EPersonService getEPersonService() {
        return epersonService;
    }

    @Override
    public GroupService getGroupService() {
        return groupService;
    }

    @Override
    public RegistrationDataService getRegistrationDataService() {
        return registrationDataService;
    }

    @Override
    public AccountService getAccountService() {
        return accountService;
    }

    @Override
    public SubscribeService getSubscribeService() {
        return subscribeService;
    }

    @Override
    public SupervisorService getSupervisorService() {
        return supervisorService;
    }
}
