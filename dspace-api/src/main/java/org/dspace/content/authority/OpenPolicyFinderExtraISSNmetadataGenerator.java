/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderJournal;

/**
 * Implementation of {@link OpenPolicyFinderExtraMetadataGenerator} that extracts
 * the first ISSN from an {@link OpenPolicyFinderJournal} and adds it as extra
 * metadata for the authority choice.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class OpenPolicyFinderExtraISSNmetadataGenerator implements OpenPolicyFinderExtraMetadataGenerator {

    private String relatedInputFormMetadata;

    /**
     * {@inheritDoc}
     * <p>
     * Extracts the first ISSN from the journal's ISSN list and maps it to the
     * configured input form metadata field.
     * </p>
     */
    @Override
    public Map<String, String> build(OpenPolicyFinderJournal journal) {
        Map<String, String> extras = new HashMap<>();
        List<String> issns = journal.getIssns();
        String value = issns.isEmpty() ? StringUtils.EMPTY : issns.get(0);
        extras.put("data-" + this.relatedInputFormMetadata, value);
        extras.put(this.relatedInputFormMetadata, value);
        return extras;
    }

    /**
     * Set the metadata field name used for mapping the ISSN value.
     *
     * @param relatedInputFormMetadata the input form metadata field name
     */
    public void setRelatedInputFormMetadata(String relatedInputFormMetadata) {
        this.relatedInputFormMetadata = relatedInputFormMetadata;
    }

}