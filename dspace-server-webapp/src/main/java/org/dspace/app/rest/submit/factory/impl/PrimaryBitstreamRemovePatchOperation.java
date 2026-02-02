/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import static org.dspace.core.Constants.CONTENT_BUNDLE_NAME;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.content.Bundle;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "remove" operation to remove primary bitstream.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class PrimaryBitstreamRemovePatchOperation extends RemovePatchOperation<String> {

    @Autowired
    private ItemService itemService;

    @Override
    void remove(Context context, HttpServletRequest request, InProgressSubmission source, String path, Object value)
         throws Exception {
        Item item = source.getItem();
        List<Bundle> bundles = itemService.getBundles(item, CONTENT_BUNDLE_NAME);
        bundles.forEach(b -> b.setPrimaryBitstreamID(null));
    }

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return null;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return null;
    }

}
