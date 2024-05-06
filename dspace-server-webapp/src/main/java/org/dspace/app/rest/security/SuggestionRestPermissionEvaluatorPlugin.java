/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.dspace.app.rest.security.DSpaceRestPermission.DELETE;
import static org.dspace.app.rest.security.DSpaceRestPermission.READ;
import static org.dspace.app.rest.security.DSpaceRestPermission.WRITE;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.SuggestionRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 *
 * An authenticated user is allowed to view a suggestion
 * related to a Target object that he owns (as defined by "dspace.object.owner" metadata field)
 * See {@link RestPermissionEvaluatorPlugin} for the inherited contract.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class SuggestionRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    @Autowired
    private ItemService itemService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission restPermission) {

        if (!READ.equals(restPermission) && !WRITE.equals(restPermission) && !DELETE.equals(restPermission)) {
            return false;
        }

        if (!StringUtils.equalsIgnoreCase(targetType, SuggestionRest.NAME)
                && !StringUtils.startsWithIgnoreCase(targetType, SuggestionRest.NAME)) {
            return false;
        }

        Context context = ContextUtil.obtainCurrentRequestContext();

        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        try {
            String id = targetId.toString();
            UUID uuid = null;
            if (id.contains(":")) {
                uuid = UUIDUtils.fromString(id.split(":", 3)[1]);
            } else {
                uuid = UUIDUtils.fromString(id);
            }
            if (uuid == null) {
                return false;
            }
            Item item = itemService.find(context, uuid);
            if (item != null) {
                List<MetadataValue> mvalues = itemService.getMetadataByMetadataString(item, "dspace.object.owner");
                if (mvalues != null) {
                    for (MetadataValue mv : mvalues) {
                        if (StringUtils.equals(mv.getAuthority(), currentUser.getID().toString())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            return false;
        }

        return false;
    }

}
