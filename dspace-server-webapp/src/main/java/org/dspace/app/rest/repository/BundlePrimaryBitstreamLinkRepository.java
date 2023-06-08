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

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "primaryBitstream" subresource of an individual bundle.
 */
@Component(BundleRest.CATEGORY + "." + BundleRest.NAME + "." + BundleRest.PRIMARY_BITSTREAM)
public class BundlePrimaryBitstreamLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    BundleService bundleService;

    /**
     * Retrieves the primaryBitstream of a Bundle.
     * Returns null if Bundle doesn't have a primaryBitstream.
     * <br><code>
     * curl -X GET "http://{dspace.server.url}/api/core/bundles/{bundle-uuid}/primaryBitstream"
     * </code>
     *
     * @param request           The HttpServletRequest if relevant
     * @param bundleId          The UUID of the Bundle
     * @param optionalPageable  The pageable if relevant
     * @param projection        The projection to use
     * @return                  The primaryBitstream, or null if not found
     */
    @PreAuthorize("hasPermission(#bundleId, 'BUNDLE', 'READ')")
    public BitstreamRest getPrimaryBitstream(@Nullable HttpServletRequest request,
                                             UUID bundleId,
                                             @Nullable Pageable optionalPageable,
                                             Projection projection) {
        try {
            Context context = obtainContext();
            Bundle bundle = bundleService.find(context, bundleId);
            if (bundle == null) {
                throw new ResourceNotFoundException("No such bundle: " + bundleId);
            }
            if (bundle.getPrimaryBitstream() == null) {
                return null;
            }
            return converter.toRest(bundle.getPrimaryBitstream(), projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets a primaryBitstream on a Bundle.
     *
     * @param context       The current DSpace context
     * @param bundleId      The UUID of the Bundle
     * @param bitstream     The Bitstream to use as primaryBitstream
     * @param projection    The projection to use
     * @return              The Bundle
     */
    @PreAuthorize("hasPermission(#bundleId, 'BUNDLE', 'WRITE')")
    public BundleRest createPrimaryBitstream(Context context, UUID bundleId,
                                             Bitstream bitstream, Projection projection) {
        try {
            Bundle bundle = setPrimaryBitstream(context, bundleId, bitstream, true);
            return converter.toRest(context.reloadEntity(bundle), projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates a primaryBitstream on a Bundle.
     *
     * @param context       The current DSpace context
     * @param bundleId      The UUID of the Bundle
     * @param bitstream     The Bitstream to use as primaryBitstream
     * @param projection    The projection to use
     * @return              The Bundle
     */
    @PreAuthorize("hasPermission(#bundleId, 'BUNDLE', 'WRITE')")
    public BundleRest updatePrimaryBitstream(Context context, UUID bundleId,
                                             Bitstream bitstream, Projection projection) {
        try {
            Bundle bundle = setPrimaryBitstream(context, bundleId, bitstream, false);
            return converter.toRest(context.reloadEntity(bundle), projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes the primaryBitstream on a Bundle.
     *
     * @param context       The current DSpace context
     * @param bundleId      The UUID of the Bundle
     */
    @PreAuthorize("hasPermission(#bundleId, 'BUNDLE', 'WRITE')")
    public void deletePrimaryBitstream(Context context, UUID bundleId) {
        try {
            Bundle bundle = setPrimaryBitstream(context, bundleId, null, false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Internal method to set the primaryBitstream on a Bundle.
     *
     * @param context       The current DSpace context
     * @param bundleId      The UUID of the Bundle
     * @param bitstream     The Bitstream to use as primaryBitstream
     * @param shouldBeSet   Whether a primaryBitstream should already be set:
     *                      primaryBitstream should be present before updating or deleting,
     *                      it should be null before adding
     * @return              The Bundle
     * @throws ResourceNotFoundException if the bundle is not found
     * @throws DSpaceBadRequestException if primaryBitstream exists during an POST,
     *                                   if primaryBitstream is null during an UPDATE or DELETE
     * @throws UnprocessableEntityException if the bundle does not contain the bitstream
     */
    private Bundle setPrimaryBitstream(Context context, UUID bundleId, Bitstream bitstream, boolean shouldBeSet)
        throws SQLException {
        Bundle bundle = bundleService.find(context, bundleId);
        if (bundle == null) {
            throw new ResourceNotFoundException("No such bundle: " + bundleId);
        }
        if (!shouldBeSet && bundle.getPrimaryBitstream() == null) {
            throw new DSpaceBadRequestException("Bundle '" + bundle.getName()
                                                    + "' does not have a primary bitstream.");
        }
        if (shouldBeSet && bundle.getPrimaryBitstream() != null) {
            throw new DSpaceBadRequestException("Bundle '" + bundle.getName()
                                                    + "' already has a primary bitstream.");
        }
        if (bitstream != null && !bundle.getBitstreams().contains(bitstream)) {
            throw new UnprocessableEntityException("Bundle '" + bundle.getName() + "' does not contain " +
                                                       "bitstream with id: " + bitstream.getID());
        }

        bundle.setPrimaryBitstreamID(bitstream);
        context.commit();
        return bundle;
    }
}
