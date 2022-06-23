/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service.impl;

import static java.util.Comparator.comparing;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.orcid.service.MetadataSignatureGenerator;

/**
 * Implementation of {@link MetadataSignatureGenerator} that composes a
 * signature made up of a section for each metadata value, divided by the
 * character SIGNATURE_SECTIONS_SEPARATOR. <br/>
 * Each section is composed of the metadata field, the metadata value and, if
 * present, the authority, divided by the character METADATA_SECTIONS_SEPARATOR.
 * <br/>
 * The presence of the metadata field allows to have different signatures for
 * metadata with the same values but referring to different fields, while the
 * authority allows to distinguish metadata that refer to different entities,
 * even if they have the same value. Finally, the various sections of the
 * signature are sorted by metadata field so that the order of the input
 * metadata values does not affect the signature.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class PlainMetadataSignatureGeneratorImpl implements MetadataSignatureGenerator {

    private static final String SIGNATURE_SECTIONS_SEPARATOR = "§§";
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
        return getSignatureSections(signature)
            .map(signatureSection -> findFirstBySignatureSection(context, item, signatureSection))
            .flatMap(metadataValue -> metadataValue.stream())
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

    private Optional<MetadataValue> findFirstBySignatureSection(Context context, Item item, String signatureSection) {
        return item.getMetadata().stream()
            .filter(metadataValue -> matchSignature(context, metadataValue, signatureSection))
            .findFirst();
    }

    private boolean matchSignature(Context context, MetadataValue metadataValue, String signatureSection) {
        return generate(context, List.of(metadataValue)).equals(signatureSection);
    }

    private Stream<String> getSignatureSections(String signature) {
        return Arrays.stream(StringUtils.split(signature, SIGNATURE_SECTIONS_SEPARATOR));
    }

    private String getField(MetadataValue metadataValue) {
        return metadataValue.getMetadataField().toString('.');
    }

    private String getValue(MetadataValue metadataValue) {
        return metadataValue.getValue() != null ? metadataValue.getValue() : "";
    }

}
