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

import org.dspace.app.rest.model.BitstreamFormatRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "format" subresource of an individual bitstream.
 */
@Component(BitstreamRest.CATEGORY + "." + BitstreamRest.NAME + "." + BitstreamRest.FORMAT)
public class BitstreamFormatLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    BitstreamService bitstreamService;

    @PreAuthorize("hasPermission(#bitstreamId, 'BITSTREAM', 'READ')")
    public BitstreamFormatRest getFormat(@Nullable HttpServletRequest request,
                                         UUID bitstreamId,
                                         @Nullable Pageable optionalPageable,
                                         Projection projection) {
        try {
            Context context = obtainContext();
            Bitstream bitstream = bitstreamService.find(context, bitstreamId);
            if (bitstream == null) {
                throw new ResourceNotFoundException("No such bitstream: " + bitstreamId);
            }
            if (bitstream.getFormat(context) == null) {
                return null;
            }
            return converter.toRest(bitstream.getFormat(context), projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
