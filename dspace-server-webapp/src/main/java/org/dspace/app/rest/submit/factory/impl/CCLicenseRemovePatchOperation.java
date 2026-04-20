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
 * Submission "remove" PATCH operation
 *
 * To remove the Creative Commons License of a workspace item.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/31599 -H "Content-Type:
 * application/json" -d '[{ "op": "remove", "path": "/sections/cclicense/uri"}]'
 * </code>
 */
public class CCLicenseRemovePatchOperation extends RemovePatchOperation<String> {

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
    void remove(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
            Object value) throws Exception {
        Item item = source.getItem();


        if (StringUtils.isNotBlank(creativeCommonsService.getLicenseName(item))) {
            creativeCommonsService.removeLicense(context, item);
        } else {
            throw new IllegalArgumentException("No CC license can be removed since none is present on submission: "
                                                       + source.getID());
        }

    }

}
