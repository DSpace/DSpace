/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderJournal;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderPublisher;

/**
 * Implementation of {@link OpenPolicyFinderExtraMetadataGenerator} that extracts
 * the publisher name from an {@link OpenPolicyFinderJournal} and adds it as extra
 * metadata for the authority choice.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class OpenPolicyFinderExtraPublisherMetadataGenerator implements OpenPolicyFinderExtraMetadataGenerator {

    private String relatedInputFormMetadata;

    /**
     * {@inheritDoc}
     * <p>
     * Extracts the publisher name from the journal and maps it to the
     * configured input form metadata field.
     * </p>
     */
    @Override
    public Map<String, String> build(OpenPolicyFinderJournal journal) {
        Map<String, String> extras = new HashMap<>();
        OpenPolicyFinderPublisher publisher = journal.getPublisher();
        String publisherName = Objects.nonNull(publisher) ? publisher.getName() : StringUtils.EMPTY;
        extras.put("data-" + this.relatedInputFormMetadata, publisherName);
        extras.put(this.relatedInputFormMetadata, publisherName);
        return extras;
    }

    /**
     * Set the metadata field name used for mapping the publisher value.
     *
     * @param relatedInputFormMetadata the input form metadata field name
     */
    public void setRelatedInputFormMetadata(String relatedInputFormMetadata) {
        this.relatedInputFormMetadata = relatedInputFormMetadata;
    }

}