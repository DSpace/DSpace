/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;
import java.util.UUID;

import org.dspace.app.rest.repository.BitstreamRestRepository;
import org.dspace.core.Context;
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

    private final static String METADATA_READ_PERMISSION = "METADATA_READ";

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                                 Object permission) {
        if (permission.toString().equalsIgnoreCase(METADATA_READ_PERMISSION)) {
            return true;
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
