/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit.util;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BulkEditUtil {
    @Autowired
    protected MetadataAuthorityService metadataAuthorityService;

    public boolean isAuthorityControlledField(String field, String separator) {
        return metadataAuthorityService.isAuthorityControlled(getCleanMdField(field, separator, "_"));
    }

    public boolean isAuthorityControlledField(String field) {
        return isAuthorityControlledField(field, ".");
    }

    public String getCleanMdField(String field, String originalSeparator, String newSeparator) {
        String mdf = field;
        if (StringUtils.contains(mdf, ":")) {
            mdf = StringUtils.substringAfter(field, ":");
        }
        if (StringUtils.contains(mdf, "[")) {
            mdf = StringUtils.substringBefore(mdf, "[");
        }
        if (!StringUtils.contains(mdf, newSeparator)) {
            mdf = mdf.replaceAll(Pattern.quote(originalSeparator), newSeparator);
        }
        return mdf;
    }

    public static BulkEditUtil getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("bulkEditUtil", BulkEditUtil.class);
    }
}
