/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.repository.BitstreamRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Used by {@link BitstreamRestRepository#findOne(Context, UUID)} to get metadata of private bitstreams even though user
 * can't access actual file
 *
 * @author Maria Verdonck (Atmire) on 15/06/2020
 */
@Component
public class BitstreamMetadataReadPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private RequestService requestService;
    @Autowired
    private DSpaceObjectUtils dspaceObjectUtil;
    @Autowired
    AuthorizeService authorizeService;
    @Autowired
    protected BitstreamService bitstreamService;

    private final static String METADATA_READ_PERMISSION = "METADATA_READ";

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                                 Object permission) {
        if (permission.toString().equalsIgnoreCase(METADATA_READ_PERMISSION) && targetId != null) {
            Request request = requestService.getCurrentRequest();
            Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

            try {
                UUID dsoUuid = UUID.fromString(targetId.toString());
                DSpaceObject dso = dspaceObjectUtil.findDSpaceObject(context, dsoUuid);
                if (dso instanceof Bitstream) {
                    return this.metadataReadPermissionOnBitstream(context, (Bitstream) dso);
                }
            } catch (SQLException e) {
                log.error(e::getMessage, e);
            }
        }
        return false;
    }

    public boolean metadataReadPermissionOnBitstream(Context context, Bitstream bitstream) throws SQLException {
        if (authorizeService.isAdmin(context, bitstream)) {
            // Is Admin on bitstream
            return true;
        }
        if (authorizeService.authorizeActionBoolean(context, bitstream, Constants.READ)) {
            // Has READ rights on bitstream
            return true;
        }
        DSpaceObject bitstreamParentObject = bitstreamService.getParentObject(context, bitstream);
        if (bitstreamParentObject instanceof Item && !(bitstream).getBundles().isEmpty()) {
            // If parent is item and it is in a bundle
            Bundle firstBundle = (bitstream).getBundles().get(0);
            if (authorizeService.authorizeActionBoolean(context, bitstreamParentObject, Constants.READ)
                && authorizeService.authorizeActionBoolean(context, firstBundle, Constants.READ)) {
                // Has READ rights on bitstream's parent item AND first bundle bitstream is in
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
                                       DSpaceRestPermission restPermission) {
        // No need to override, since only user by super.hasPermission(Authentication authentication, Serializable
        // targetId, String targetType, Object permission) which is overrode above
        return false;
    }
}
