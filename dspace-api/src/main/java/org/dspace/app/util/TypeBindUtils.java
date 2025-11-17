/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Utility methods for the type bind functionality.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 *
 */
public class TypeBindUtils {

    private static final ConfigurationService configurationService = DSpaceServicesFactory
        .getInstance().getConfigurationService();
    private static final ItemService itemService = ContentServiceFactory
        .getInstance().getItemService();
    private static final MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getMetadataAuthorityService();

    private TypeBindUtils() {
    }

    /**
     * This method gets the field used for type-bind.
     *
     * @return the field used for type-bind.
     */
    public static String getTypeBindField() {
        return configurationService.getProperty("submit.type-bind.field", "dc.type");
    }

    /**
     * Gets the value of the type-bind field from the current item in the submission.
     *
     * @param obj the in-progress submission
     * @return the value of the type-bind field, or null if not set
     */
    public static String getTypeBindValue(InProgressSubmission<?> obj) {
        List<MetadataValue> documentType = itemService.getMetadataByMetadataString(
            obj.getItem(), getTypeBindField());

        // check empty type-bind field
        if (documentType == null || documentType.isEmpty()
            || StringUtils.isBlank(documentType.get(0).getValue())) {
            return null;
        }

        return documentType.get(0).getValue();
    }

}
