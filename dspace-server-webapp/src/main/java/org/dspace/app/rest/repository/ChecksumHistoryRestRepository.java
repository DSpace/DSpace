/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.ChecksumHistoryRest;
import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.checker.service.ChecksumHistoryService;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(ChecksumHistoryRest.CATEGORY + "." + ChecksumHistoryRest.PLURAL_NAME)
public class ChecksumHistoryRestRepository extends DSpaceRestRepository<ChecksumHistoryRest, Integer> {

    @Autowired
    private ChecksumHistoryService checksumHistoryService;

    @Autowired
    private BitstreamService bitstreamService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ChecksumHistoryRest findOne(Context context, Integer id) {
        try {
            ChecksumHistory checksumHistory = checksumHistoryService.find(context, id.longValue());
            if (checksumHistory == null) {
                return null;
            }
            return converter.toRest(checksumHistory, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ChecksumHistoryRest> findAll(Context context, Pageable pageable) {
        try {
            int total = checksumHistoryService.countTotal(context);
            List<ChecksumHistory> historyList = checksumHistoryService.findAll(
                    context, pageable.getPageSize(), Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(historyList, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "byBitstream")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ChecksumHistoryRest> findByBitstream(
            @Parameter(value = "bitstream", required = true) UUID bitstreamId,
            Pageable pageable) {
        try {
            Context context = obtainContext();
            Bitstream bitstream = bitstreamService.find(context, bitstreamId);
            if (bitstream == null) {
                throw new ResourceNotFoundException("No bitstream found with id: " + bitstreamId);
            }
            int total = checksumHistoryService.countByBitstream(context, bitstream);
            List<ChecksumHistory> historyList = checksumHistoryService.findByBitstream(
                    context, bitstream, pageable.getPageSize(), Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(historyList, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "byResultCode")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ChecksumHistoryRest> findByResultCode(
            @Parameter(value = "resultCode", required = true) String resultCode,
            Pageable pageable) {
        try {
            Context context = obtainContext();
            ChecksumResultCode code;
            try {
                code = ChecksumResultCode.valueOf(resultCode);
            } catch (IllegalArgumentException e) {
                throw new DSpaceBadRequestException("Invalid ChecksumResultCode: " + resultCode);
            }
            int total = checksumHistoryService.countByResultCode(context, code);
            List<ChecksumHistory> historyList = checksumHistoryService.findByResultCode(
                    context, code, pageable.getPageSize(), Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(historyList, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<ChecksumHistoryRest> getDomainClass() {
        return ChecksumHistoryRest.class;
    }
}