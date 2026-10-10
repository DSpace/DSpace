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

import org.apache.commons.lang3.StringUtils;
import org.dspace.authority.AuthorityValue;

/**
 * {@link ZDBExtraMetadataGenerator} implementation that extracts the journal
 * title (and optionally the authority string) from a ZDB {@link AuthorityValue}
 * and maps it to a related input-form metadata field.
 *
 * <p>If an authority string is available, the value is stored as
 * {@code title::authority}; otherwise only the title is used.</p>
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public class ZDBExtraTitleMetadataGenerator implements ZDBExtraMetadataGenerator {
    private String relatedInputformMetadata;

    /** {@inheritDoc} */
    @Override
    public Map<String, String> build(AuthorityValue val) {
        Map<String, String> extras = new HashMap<String, String>();

        String title = val.getValue();
        if (title != null) {
            String authority = val.generateString();
            if (StringUtils.isNotBlank(authority)) {
                extras.put("data-" + getRelatedInputformMetadata(), title + "::" + authority);
            } else {
                extras.put("data-" + getRelatedInputformMetadata(), title);
            }
            return extras;
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