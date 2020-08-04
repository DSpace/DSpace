/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import org.apache.commons.lang3.BooleanUtils;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "add" PATCH operation
 *
 * To accept/reject the license.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/31599 -H "Content-Type:
 * application/json" -d '[{ "op": "add", "path": "/sections/license/granted", "value":"true"}]'
 * </code>
 *
 * Please note that according to the JSON Patch specification RFC6902 a
 * subsequent add operation on the "granted" path will have the effect to
 * replace the previous granted license with a new one.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class LicenseAddPatchOperation extends AddPatchOperation<String> {

    @Autowired
    ItemService itemService;

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return String.class;
    }

    @Override
    void add(Context context, Request currentRequest, InProgressSubmission source, String path, Object value)
        throws Exception {

        Boolean grant = null;
        // we are friendly with the client and accept also a string representation for the boolean
        if (value instanceof String) {
            grant = BooleanUtils.toBooleanObject((String) value);
        } else {
            grant = (Boolean) value;
        }

        if (grant == null) {
            throw new IllegalArgumentException(
                "Value is not a valid boolean expression (permitted value: on/off, true/false and yes/no");
        }

        Item item = source.getItem();
        EPerson submitter = context.getCurrentUser();

        // remove any existing DSpace license (just in case the user
        // accepted it previously)
        itemService.removeDSpaceLicense(context, item);

        if (grant) {
            String license = LicenseUtils.getLicenseText(context.getCurrentLocale(), source.getCollection(), item,
                                                         submitter);

            LicenseUtils.grantLicense(context, item, license, null);
        }
    }

}
