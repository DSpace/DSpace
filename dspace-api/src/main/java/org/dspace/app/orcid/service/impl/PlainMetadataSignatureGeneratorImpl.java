/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service.impl;

import static java.util.Comparator.comparing;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.orcid.service.MetadataSignatureGenerator;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

/**
 * Implementation of {@link MetadataSignatureGenerator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class PlainMetadataSignatureGeneratorImpl implements MetadataSignatureGenerator {

    private static final String SIGNATURE_SECTIONS_SEPARATOR = "/";
    private static final String METADATA_SECTIONS_SEPARATOR = "::";

    @Override
    public String generate(Context context, List<MetadataValue> metadataValues) {
        return metadataValues.stream()
            .sorted(comparing(metadataValue -> metadataValue.getMetadataField().getID()))
            .map(this::composeSignatureSection)
            .collect(Collectors.joining(SIGNATURE_SECTIONS_SEPARATOR));
    }

    @Override
    public List<MetadataValue> findBySignature(Context context, Item item, String signature) {
        String[] signatureSections = getSignatureSections(signature);
        return item.getMetadata().stream()
            .filter(metadataValue -> matchSignature(context, metadataValue, signatureSections))
            .collect(Collectors.toList());
    }

    private String composeSignatureSection(MetadataValue metadataValue) {
        String fieldId = getField(metadataValue);
        String metadataValueSignature = fieldId + METADATA_SECTIONS_SEPARATOR + getValue(metadataValue);
        if (StringUtils.isNotBlank(metadataValue.getAuthority())) {
            return metadataValueSignature + METADATA_SECTIONS_SEPARATOR + metadataValue.getAuthority();
        } else {
            return metadataValueSignature;
        }
    }

    private boolean matchSignature(Context context, MetadataValue metadataValue, String[] signatureSections) {
        return Arrays.stream(signatureSections)
            .anyMatch(signatureSection -> generate(context, List.of(metadataValue)).equals(signatureSection));
    }

    private String[] getSignatureSections(String signature) {
        return StringUtils.split(signature, SIGNATURE_SECTIONS_SEPARATOR);
    }

    private String getField(MetadataValue metadataValue) {
        return metadataValue.getMetadataField().toString('.');
    }

    private String getValue(MetadataValue metadataValue) {
        return metadataValue.getValue() != null ? metadataValue.getValue() : "";
    }

}
