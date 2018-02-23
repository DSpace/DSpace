/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import org.apache.commons.lang.BooleanUtils;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "replace" patch operation
 *
 * {@link LicenseAddPatchOperation}
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class LicenseReplacePatchOperation extends ReplacePatchOperation<String> {

    @Autowired
    ItemService itemService;

    @Override
    void replace(Context context, Request currentRequest, WorkspaceItem source, String path, Object value)
        throws Exception {

        Boolean grant = BooleanUtils.toBooleanObject((String) value);

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

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return String.class;
    }

}
