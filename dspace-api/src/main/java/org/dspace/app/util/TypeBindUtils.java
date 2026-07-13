/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.List;

import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Utility methods for the type bind functionality.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 *
 */
@Component
public class TypeBindUtils {

    @Autowired
    protected ConfigurationService configurationService;
    @Autowired
    protected ItemService itemService;
    @Autowired
    protected MetadataAuthorityService metadataAuthorityService;

    /**
     * This method gets the field used for type-bind.
     *
     * @return the field used for type-bind.
     */
    public String getTypeBindField() {
        return configurationService.getProperty("submit.type-bind.field", "dc.type");
    }

    /**
     * Gets all metadata values of the type-bind field from the current item in the submission.
     *
     * @param obj the in-progress submission
     * @return the list of MetadataValue objects of the type-bind field
     */
    public List<MetadataValue> getTypeBindMetadataValues(InProgressSubmission<?> obj) {
        return itemService.getMetadataByMetadataString(obj.getItem(), getTypeBindField());
    }

}
