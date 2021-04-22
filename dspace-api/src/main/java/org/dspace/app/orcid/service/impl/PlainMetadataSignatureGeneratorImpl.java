/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.orcid.service.MetadataSignatureGenerator;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.CollectionService;
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

    @Autowired
    private CollectionService collectionService;

    private DCInputsReader reader;

    @PostConstruct
    public void postConstruct() throws DCInputsReaderException {
        this.reader = new DCInputsReader();
    }

    @Override
    public List<String> generate(Context context, Item item, String metadataField) throws SQLException {

        List<String> metadataGroup = getMetadataGroup(context, item, metadataField);
        if (CollectionUtils.isEmpty(metadataGroup)) {
            return generate(context, item, List.of(metadataField));
        } else {
            return generate(context, item, metadataGroup);
        }
    }

    private List<String> generate(Context context, Item item, List<String> metadataFields) {

        List<String> signatures = new ArrayList<String>();

        Map<String, List<MetadataValue>> metadataFieldMap = new LinkedHashMap<>();
        int groupSize = -1;
        for (String metadataField : metadataFields) {
            List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, metadataField);
            groupSize = groupSize != -1 ? metadataValues.size() : groupSize;
            metadataFieldMap.put(metadataField, metadataValues);
        }

        for (int currentPlace = 0; currentPlace < groupSize; currentPlace++) {
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

    private List<String> getMetadataGroup(Context context, Item item, String groupName) throws SQLException {
        Collection collection = collectionService.findByItem(context, item);
        try {
            return this.reader.getAllNestedMetadataByGroupName(collection, groupName);
        } catch (DCInputsReaderException e) {
            return Collections.emptyList();
        }
    }

}
