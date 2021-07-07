package org.dspace.content;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Predicate;

/**
 *
 * @author Alba Aliu
 */
public class MetadataSecurityEvaluationLevel2 implements MetadataSecurityEvaluation {
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private   DSpaceObjectServiceImpl<Item> dSpaceObjectServiceImpl;
    @Autowired
    private ItemService itemService;
    /**
     *
     * @return true/false if the user can/'t see the metadata
     * @param context The context of the app
     * @param item The Item for which the user wants to see the metadata
     * @param metadataField The metadata field related with a metadata value
     */
    @Override
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField) throws SQLException {
        if (context != null && authorizeService.isAdmin(context)) {
            List<MetadataValue> owners = itemService.getMetadataByMetadataString(item, "cris.owner");
            Predicate<MetadataValue> checkOwner = v -> StringUtils.equals(v.getAuthority(), context.getCurrentUser().id+"");
            return owners.stream().anyMatch(checkOwner);
        }
        return false;
    }
}
