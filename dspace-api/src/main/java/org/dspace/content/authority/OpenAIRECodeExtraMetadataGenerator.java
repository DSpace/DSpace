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
/**
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class OpenAIRECodeExtraMetadataGenerator implements OpenAIREExtraMetadataGenerator {
    private String relatedInputformMetadata = "dc_relation_grantno";

    @Override
    public Map<String, String> build(String value) {
        Map<String, String> extras = new HashMap<String, String>();
        extras.put("data-" + getRelatedInputformMetadata(), value);
        return extras;
    }

    public String getRelatedInputformMetadata() {
        return relatedInputformMetadata;
    }

    public void setRelatedInputformMetadata(String relatedInputformMetadata) {
        this.relatedInputformMetadata = relatedInputformMetadata;
    }

}