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
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class ZDBExtraIssnMetadataGenerator implements ZDBExtraMetadataGenerator {
    private String relatedInputformMetadata;

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

    public void setRelatedInputformMetadata(String relatedInputformMetadata) {
        this.relatedInputformMetadata = relatedInputformMetadata;
    }

    public String getRelatedInputformMetadata() {
        return relatedInputformMetadata;
    }
}