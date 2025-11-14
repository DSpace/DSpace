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
 * Operation to remove redirected URL.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class RedirectedUrlRemoveOperation extends RemovePatchOperation<CustomUrl> {

    @Autowired
    private CustomUrlService customUrlService;

    @Override
    @SuppressWarnings("rawtypes")
    void remove(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
                Object value) throws Exception {

        Item item = source.getItem();
        int index = calculateRemoveIndex(path);

        if (index == -1) {
            customUrlService.deleteAllOldCustomUrls(context, item);
        } else {
            customUrlService.deleteOldCustomUrlByIndex(context, item, index);
        }

    }

    private int calculateRemoveIndex(String path) {
        String absolutePath = getAbsolutePath(path);
        String[] splittedPath = absolutePath.split("/");
        return splittedPath.length == 1 ? -1 : Integer.valueOf(splittedPath[1]);
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
