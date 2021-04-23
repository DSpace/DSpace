/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.orcid.service.MetadataSignatureGenerator;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link MetadataSignatureGenerator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class PlainMetadataSignatureGeneratorImpl implements MetadataSignatureGenerator {

    @Autowired
    private ItemService itemService;

    @Override
    public List<String> generate(Context context, Item item, List<String> metadataFields) {

        List<String> signatures = new ArrayList<String>();

        Map<String, List<MetadataValue>> metadataFieldMap = new LinkedHashMap<>();
        int maxGroupSize = -1;
        for (String metadataField : metadataFields) {
            List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, metadataField);
            maxGroupSize = metadataValues.size() > maxGroupSize ? metadataValues.size() : maxGroupSize;
            metadataFieldMap.put(metadataField, metadataValues);
        }

        for (int currentPlace = 0; currentPlace < maxGroupSize; currentPlace++) {
            List<String> signatureSections = new ArrayList<>();
            for (String metadataField : metadataFields) {
                List<MetadataValue> metadataValues = metadataFieldMap.get(metadataField);
                if (metadataValues.size() <= currentPlace) {
                    signatureSections.add("#");
                } else {
                    signatureSections.add(String.valueOf(metadataValues.get(currentPlace).getID()));
                }
            }
            signatures.add(String.join("/", signatureSections));
        }

        return signatures;

    }

}
