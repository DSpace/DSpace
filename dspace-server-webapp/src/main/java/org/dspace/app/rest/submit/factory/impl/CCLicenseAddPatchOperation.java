/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.license.service.CreativeCommonsService;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Submission "add" PATCH operation
 *
 * To add or update the Creative Commons License of a workspace item.
 * When the item already has a Creative Commons License, the license will be replaced with a new one.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/31599 -H "Content-Type:
 * application/json" -d '[{ "op": "add", "path": "/sections/cclicense/uri",
 * "value":"https://creativecommons.org/licenses/by-nc-sa/3.0/us/"}]'
 * </code>
 */
public class CCLicenseAddPatchOperation extends AddPatchOperation<String> {

    @Autowired
    CreativeCommonsService creativeCommonsService;

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return String.class;
    }

    @Override
    void add(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path, Object value)
            throws Exception {
        String licenseUri = null;
        if (value instanceof String) {
            licenseUri = (String) value;
        }

        if (StringUtils.isBlank(licenseUri)) {
            throw new IllegalArgumentException(
                    "Value is not a valid license URI");
        }

        Item item = source.getItem();
        boolean updateLicense = creativeCommonsService.updateLicense(context, licenseUri, item);
        if (!updateLicense) {
            throw new IllegalArgumentException("The license uri: " + licenseUri + ", could not be resolved to a " +
                                                       "CC license");
        }
    }

}
