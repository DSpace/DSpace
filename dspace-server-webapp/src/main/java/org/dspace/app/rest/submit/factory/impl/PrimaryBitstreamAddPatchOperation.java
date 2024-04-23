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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "add" operation to set primary bitstream.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class PrimaryBitstreamAddPatchOperation extends AddPatchOperation<String> {

    @Autowired
    private ItemService itemService;

    @Override
    void add(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path, Object value)
        throws Exception {
        Item item = source.getItem();
        UUID primaryUUID = parseValue(value);
        List<Bundle> bundles = itemService.getBundles(item, CONTENT_BUNDLE_NAME);
        Optional<Bundle> currentPrimaryBundle = bundles.stream()
                                                       .filter(bundle -> Objects.nonNull(bundle.getPrimaryBitstream()))
                                                       .findFirst();

        Optional<Bitstream> primaryBitstreamToAdd = null;
        for (Bundle bundle : bundles) {
            primaryBitstreamToAdd = bundle.getBitstreams().stream()
                                                          .filter(b -> b.getID().equals(primaryUUID))
                                                          .findFirst();
            if (primaryBitstreamToAdd.isPresent()) {
                if (currentPrimaryBundle.isPresent()) {
                    currentPrimaryBundle.get().setPrimaryBitstreamID(null);
                }
                bundle.setPrimaryBitstreamID(primaryBitstreamToAdd.get());
                break;
            }
        }

        if (primaryBitstreamToAdd.isEmpty()) {
            throw new UnprocessableEntityException("The provided uuid: " + primaryUUID +
                                                   " of bitstream to set as primary doesn't match any bitstream!");
        }
    }

    private UUID parseValue(Object value) {
        UUID primaryBitstreamUUID;
        try {
            primaryBitstreamUUID = UUID.fromString((String) value);
        } catch (Exception e) {
            throw new UnprocessableEntityException("The provided value is invalid!", e);
        }
        return primaryBitstreamUUID;
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
