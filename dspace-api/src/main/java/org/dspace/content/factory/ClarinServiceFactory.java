/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.factory;

import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.content.service.clarin.ClarinLicenseResourceUserAllowanceService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.content.service.clarin.ClarinUserMetadataService;
import org.dspace.content.service.clarin.ClarinUserRegistrationService;
import org.dspace.handle.service.HandleClarinService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the clarin package, use ClarinServiceFactory.getInstance() to retrieve an
 * implementation
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public abstract class ClarinServiceFactory {

    public abstract ClarinLicenseService getClarinLicenseService();

    public abstract ClarinLicenseLabelService getClarinLicenseLabelService();

    public abstract ClarinLicenseResourceMappingService getClarinLicenseResourceMappingService();

    public abstract HandleClarinService getClarinHandleService();

    public abstract ClarinUserRegistrationService getClarinUserRegistration();

    public abstract ClarinUserMetadataService getClarinUserMetadata();

    public abstract ClarinLicenseResourceUserAllowanceService getClarinLicenseResourceUserAllowance();

    public static ClarinServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("clarinServiceFactory", ClarinServiceFactory.class);
    }
}
