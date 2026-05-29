/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.access.status.DefaultAccessStatusHelper;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.app.rest.model.AccessStatusRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.AccessStatus;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for calculating the access status of a Bitstream,
 * including the embargo date
 */
@Component(BitstreamRest.CATEGORY + "." + BitstreamRest.PLURAL_NAME + "." + BitstreamRest.ACCESS_STATUS)
public class BitstreamAccessStatusLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    AccessStatusService accessStatusService;

    @PreAuthorize("hasPermission(#bitstreamId, 'BITSTREAM', 'METADATA_READ')")
    public AccessStatusRest getAccessStatus(@Nullable HttpServletRequest request,
                                            UUID bitstreamId,
                                            @Nullable Pageable optionalPageable,
                                            Projection projection) {
        try {
            Context context = obtainContext();
            Bitstream bitstream = bitstreamService.find(context, bitstreamId);
            if (bitstream == null) {
                throw new ResourceNotFoundException("No such bitstream: " + bitstreamId);
            }
            AccessStatusRest accessStatusRest = new AccessStatusRest();
            AccessStatus accessStatus = accessStatusService.getAccessStatus(context, bitstream);
            String status = accessStatus.getStatus();
            if (status == DefaultAccessStatusHelper.EMBARGO) {
                LocalDate availabilityDate = accessStatus.getAvailabilityDate();
                String embargoDate = availabilityDate.toString();
                accessStatusRest.setEmbargoDate(embargoDate);
            }
            accessStatusRest.setStatus(status);
            return accessStatusRest;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
