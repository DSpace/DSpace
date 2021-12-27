/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.customurl.CustomUrlService;
import org.dspace.app.rest.model.step.CustomUrl;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Operation to replace custom defined URL.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CustomUrlReplaceOperation extends ReplacePatchOperation<CustomUrl> {

    @Autowired
    private CustomUrlService customUrlService;

    @Override
    @SuppressWarnings("rawtypes")
    void replace(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
                 Object value) throws Exception {

        Item item = source.getItem();

        String newUrl = (String) value;
        Optional<String> currentUrl = customUrlService.getCustomUrl(item);

        if (currentUrl.isPresent() && currentUrl.get().equals(newUrl)) {
            return;
        }

        if (currentUrl.isPresent() && isBlank(newUrl) && isNotYetDeposited(item)) {
            customUrlService.deleteCustomUrl(context, item);
            customUrlService.deleteAllOldCustomUrls(context, item);
            return;
        }

        customUrlService.deleteAnyOldCustomUrlEqualsTo(context, item, newUrl);
        customUrlService.replaceCustomUrl(context, item, newUrl);

    }

    private boolean isNotYetDeposited(Item item) {
        return !item.isArchived() && !item.isWithdrawn();
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
