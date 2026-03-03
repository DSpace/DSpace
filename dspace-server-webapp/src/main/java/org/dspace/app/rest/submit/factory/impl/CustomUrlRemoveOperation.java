/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.customurl.CustomUrlService;
import org.dspace.app.rest.model.step.CustomUrl;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Operation to remove custom defined URL.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CustomUrlRemoveOperation extends RemovePatchOperation<CustomUrl> {

    @Autowired
    private CustomUrlService customUrlService;

    @Override
    @SuppressWarnings("rawtypes")
    void remove(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
                 Object value) throws Exception {

        Item item = source.getItem();
        customUrlService.deleteCustomUrl(context, item);
        customUrlService.deleteAllOldCustomUrls(context, item);

    }

    @Override
    protected Class<CustomUrl[]> getArrayClassForEvaluation() {
        return CustomUrl[].class;
    }

    @Override
    protected Class<CustomUrl> getClassForEvaluation() {
        return CustomUrl.class;
    }

}
