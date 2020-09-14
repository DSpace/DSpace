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

import org.apache.commons.lang.StringUtils;
import org.dspace.authority.AuthorityValue;

/**
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class ZDBExtraTitleMetadataGenerator implements ZDBExtraMetadataGenerator {
    private String relatedInputformMetadata;

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

    public void setRelatedInputformMetadata(String relatedInputformMetadata) {
        this.relatedInputformMetadata = relatedInputformMetadata;
    }

    public String getRelatedInputformMetadata() {
        return relatedInputformMetadata;
    }
}