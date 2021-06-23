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
 * Link repository for the thumbnail Bitstream of a Bitstream
 */
@Component(BitstreamRest.CATEGORY + "." + BitstreamRest.NAME + "." + BitstreamRest.THUMBNAIL)
public class BitstreamThumbnailLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {
    @Autowired
    BitstreamService bitstreamService;

    @PreAuthorize("hasPermission(#bitstreamId, 'BITSTREAM', 'READ')")
    public BitstreamRest getThumbnail(@Nullable HttpServletRequest request,
                                      UUID bitstreamId,
                                      @Nullable Pageable optionalPageable,
                                      Projection projection) {
        try {
            Context context = obtainContext();
            Bitstream bitstream = bitstreamService.find(context, bitstreamId);
            if (bitstream == null) {
                throw new ResourceNotFoundException("No such bitstream: " + bitstreamId);
            }
            Bitstream thumbnail = bitstreamService.getThumbnail(context, bitstream);
            if (thumbnail == null) {
                return null;
            }
            return converter.toRest(thumbnail, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
