/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.LicenseRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.core.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Controller for exposition of license
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component(CollectionRest.CATEGORY + "." + CollectionRest.NAME + "." + CollectionRest.LICENSE)
public class LicenseRestLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    CollectionService collectionService;

    @Autowired
    LicenseService licenseService;

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public LicenseRest getLicenseCollection(HttpServletRequest request, UUID uuid, Pageable pageable,
                                            Projection projection)
        throws Exception {
        Context context = obtainContext();
        Collection collection = collectionService.find(context, uuid);

        LicenseRest licenseRest = new LicenseRest();
        String text = collection.getLicenseCollection();
        if (StringUtils.isNotBlank(text)) {
            licenseRest.setCustom(true);
            licenseRest.setText(text);
        } else {
            licenseRest.setText(licenseService.getDefaultSubmissionLicense());
        }

        return licenseRest;
    }

    @Override
    public boolean isEmbeddableRelation(Object data, String name) {
        return false;
    }
}
