/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service.impl;

import java.util.List;
import java.util.stream.Collectors;

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

    private ItemService itemService;

    @Autowired
    public PlainMetadataSignatureGeneratorImpl(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public String generate(Context context, Item item, List<String> metadataFields) {
        return metadataFields.stream()
            .flatMap(metadataField -> itemService.getMetadataByMetadataString(item, metadataField).stream())
            .map(MetadataValue::getID)
            .sorted()
            .map(String::valueOf)
            .collect(Collectors.joining("/"));
    }

}
