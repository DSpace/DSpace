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

import org.dspace.authority.AuthorityValue;

/**
 * {@link ZDBExtraMetadataGenerator} implementation that extracts the ISSN
 * from a ZDB {@link AuthorityValue}'s other metadata and maps it to a
 * related input-form metadata field.
 *
 * <p>The ISSN value is read from the {@code journalIssn} key of
 * {@link AuthorityValue#getOtherMetadata()} and stored with a
 * {@code data-} prefix.</p>
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public class ZDBExtraIssnMetadataGenerator implements ZDBExtraMetadataGenerator {
    private String relatedInputformMetadata;

    /** {@inheritDoc} */
    @Override
    public Map<String, String> build(AuthorityValue val) {
        Map<String, String> extras = new HashMap<String, String>();

        Map<String, List<String>> otherMetadata = val.getOtherMetadata();
        if (otherMetadata != null && !otherMetadata.isEmpty()) {
            List<String> issns = otherMetadata.get("journalIssn");
            if (issns != null && !issns.isEmpty()) {
                for (String issn : issns) {
                    extras.put("data-" + getRelatedInputformMetadata(), issn);
                }
                return extras;
            }
        }

        extras.put("data-" + getRelatedInputformMetadata(), "");
        return extras;
    }

    /**
     * Set the target input-form metadata field name.
     *
     * @param relatedInputformMetadata the metadata field name to use
     */
    public void setRelatedInputformMetadata(String relatedInputformMetadata) {
        this.relatedInputformMetadata = relatedInputformMetadata;
    }

    /**
     * Return the target input-form metadata field name.
     *
     * @return the configured metadata field name
     */
    public String getRelatedInputformMetadata() {
        return relatedInputformMetadata;
    }
}