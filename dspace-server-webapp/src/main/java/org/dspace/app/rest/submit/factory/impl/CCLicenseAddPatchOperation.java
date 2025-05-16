/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.license.CreativeCommonsServiceImpl;
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
 * "value":"https://creativecommons.org/licenses/by-nc-sa/4.0/us/"}]'
 * </code>
 */
public class CCLicenseAddPatchOperation extends AddPatchOperation<String> {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(CreativeCommonsServiceImpl.class);

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
    void add(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path, Object licensemap)
            throws Exception {
        String licenseRights = "";
        String licenseUri = "";
        if (licensemap instanceof String){
            licenseUri = (String) licensemap;
        }
        else if (licensemap instanceof JsonValueEvaluator) {
            JsonNode cclicense = ((JsonValueEvaluator) licensemap).getValueNode();
            licenseUri = cclicense.get("uri").asText();
            licenseRights = cclicense.get("rights").asText();
        }
        if (StringUtils.isBlank(licenseUri) && StringUtils.isBlank(licenseRights)) {
            throw new IllegalArgumentException("Values for dc.rights and dc.rights.uri cannot both be empty.");
        }
        Item item = source.getItem();
        boolean updateLicense = creativeCommonsService.updateLicense(context, licenseUri, licenseRights, item);
        if (!updateLicense) {
            throw new IllegalArgumentException("The license uri: " + licenseUri + ", could not be resolved to a " +
                                                       "CC license");
        }
    }

}
