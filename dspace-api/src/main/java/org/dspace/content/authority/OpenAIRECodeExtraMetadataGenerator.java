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
 * {@link OpenAIREExtraMetadataGenerator} implementation that maps an OpenAIRE
 * project code to a related input-form metadata field.
 *
 * <p>The generated extra is stored with a {@code data-} prefix using the
 * configured {@link #relatedInputformMetadata} key (defaults to
 * {@code dc_relation_grantno}).</p>
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public class OpenAIRECodeExtraMetadataGenerator implements OpenAIREExtraMetadataGenerator {
    private String relatedInputformMetadata = "dc_relation_grantno";

    /** {@inheritDoc} */
    @Override
    public Map<String, String> build(String value) {
        Map<String, String> extras = new HashMap<String, String>();
        extras.put("data-" + getRelatedInputformMetadata(), value);
        return extras;
    }

    /**
     * Return the target input-form metadata field name.
     *
     * @return the configured metadata field name
     */
    public String getRelatedInputformMetadata() {
        return relatedInputformMetadata;
    }

    /**
     * Set the target input-form metadata field name.
     *
     * @param relatedInputformMetadata the metadata field name to use
     */
    public void setRelatedInputformMetadata(String relatedInputformMetadata) {
        this.relatedInputformMetadata = relatedInputformMetadata;
    }

}