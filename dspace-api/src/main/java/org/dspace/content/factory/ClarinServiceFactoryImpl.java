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
import org.dspace.content.service.clarin.ClarinVerificationTokenService;
import org.dspace.handle.service.HandleClarinService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the clarin package, use ClarinServiceFactory.getInstance()
 * to retrieve an implementation
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinServiceFactoryImpl extends ClarinServiceFactory {

    @Autowired(required = true)
    private ClarinLicenseService clarinLicenseService;

    @Autowired(required = true)
    private ClarinLicenseLabelService clarinLicenseLabelService;

    @Autowired(required = true)
    private ClarinLicenseResourceMappingService clarinLicenseResourceMappingService;

    @Autowired(required = true)
    private HandleClarinService handleClarinService;

    @Autowired(required = true)
    private ClarinUserRegistrationService clarinUserRegistrationService;

    @Autowired(required = true)
    private ClarinUserMetadataService clarinUserMetadataService;

    @Autowired(required = true)
    private ClarinLicenseResourceUserAllowanceService clarinLicenseResourceUserAllowanceService;

    @Autowired(required = true)
    private ClarinVerificationTokenService clarinVerificationTokenService;

    @Override
    public ClarinLicenseService getClarinLicenseService() {
        return clarinLicenseService;
    }

    @Override
    public ClarinLicenseLabelService getClarinLicenseLabelService() {
        return clarinLicenseLabelService;
    }

    @Override
    public ClarinLicenseResourceMappingService getClarinLicenseResourceMappingService() {
        return clarinLicenseResourceMappingService;
    }

    @Override
    public HandleClarinService getClarinHandleService() {
        return handleClarinService;
    }

    @Override
    public ClarinUserRegistrationService getClarinUserRegistration() {
        return clarinUserRegistrationService;
    }

    @Override
    public ClarinUserMetadataService getClarinUserMetadata() {
        return clarinUserMetadataService;
    }

    @Override
    public ClarinLicenseResourceUserAllowanceService getClarinLicenseResourceUserAllowance() {
        return clarinLicenseResourceUserAllowanceService;
    }

    @Override
    public ClarinVerificationTokenService getClarinVerificationTokenService() {
        return clarinVerificationTokenService;
    }
}
