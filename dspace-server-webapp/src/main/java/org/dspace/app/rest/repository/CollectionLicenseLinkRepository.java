/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;
import javax.annotation.Nullable;
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
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "license" subresource of an individual collection.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component(CollectionRest.CATEGORY + "." + CollectionRest.NAME + "." + CollectionRest.LICENSE)
public class CollectionLicenseLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    CollectionService collectionService;

    @Autowired
    LicenseService licenseService;

    @PreAuthorize("hasPermission(#collectionId, 'COLLECTION', 'READ')")
    public LicenseRest getLicense(@Nullable HttpServletRequest request,
                                  UUID collectionId,
                                  @Nullable Pageable pageable,
                                  Projection projection) {
        try {
            Context context = obtainContext();
            Collection collection = collectionService.find(context, collectionId);
            if (collection == null) {
                throw new ResourceNotFoundException("No such collection: " + collectionId);
            }
            LicenseRest licenseRest = new LicenseRest();
            String text = collection.getLicenseCollection();
            if (StringUtils.isNotBlank(text)) {
                licenseRest.setCustom(true);
                licenseRest.setText(text);
            } else {
                licenseRest.setText(licenseService.getDefaultSubmissionLicense());
            }
            return licenseRest;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
