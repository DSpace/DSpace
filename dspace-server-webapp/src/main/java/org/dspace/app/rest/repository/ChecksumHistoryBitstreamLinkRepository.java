/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ChecksumHistoryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.service.ChecksumHistoryService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(ChecksumHistoryRest.CATEGORY + "." + ChecksumHistoryRest.PLURAL_NAME + "." + ChecksumHistoryRest.BITSTREAM)
public class ChecksumHistoryBitstreamLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    private ChecksumHistoryService checksumHistoryService;

    @PreAuthorize("hasAuthority('ADMIN')")
    public BitstreamRest getBitstream(@Nullable HttpServletRequest request,
                                      Integer checksumHistoryId,
                                      @Nullable Pageable optionalPageable,
                                      Projection projection) {
        try {
            Context context = obtainContext();
            ChecksumHistory checksumHistory = checksumHistoryService.find(context, checksumHistoryId.longValue());
            if (checksumHistory == null) {
                throw new ResourceNotFoundException("No such checksum history record: " + checksumHistoryId);
            }
            if (checksumHistory.getBitstream() == null) {
                return null;
            }
            return converter.toRest(checksumHistory.getBitstream(), projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}