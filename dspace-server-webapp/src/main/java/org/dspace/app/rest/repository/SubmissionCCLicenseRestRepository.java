/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;

import org.dspace.app.rest.model.SubmissionCCLicenseRest;
import org.dspace.core.Context;
import org.dspace.license.CCLicense;
import org.dspace.license.service.CreativeCommonsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository that is responsible to manage CCLicense Rest objects
 */
@Component(SubmissionCCLicenseRest.CATEGORY + "." + SubmissionCCLicenseRest.NAME)
public class SubmissionCCLicenseRestRepository extends DSpaceRestRepository<SubmissionCCLicenseRest, String> {

    @Autowired
    protected CreativeCommonsService creativeCommonsService;

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public SubmissionCCLicenseRest findOne(final Context context, final String licenseId) {
        CCLicense ccLicense = creativeCommonsService.findOne(licenseId);
        if (ccLicense == null) {
            throw new ResourceNotFoundException("No CC license could be found for ID: " + licenseId );
        }
        return converter.toRest(ccLicense, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<SubmissionCCLicenseRest> findAll(final Context context, final Pageable pageable) {

        List<CCLicense> allCCLicenses = creativeCommonsService.findAllCCLicenses();
        return converter.toRestPage(allCCLicenses, pageable, utils.obtainProjection());
    }

    @Override
    public Class<SubmissionCCLicenseRest> getDomainClass() {
        return SubmissionCCLicenseRest.class;
    }
}
