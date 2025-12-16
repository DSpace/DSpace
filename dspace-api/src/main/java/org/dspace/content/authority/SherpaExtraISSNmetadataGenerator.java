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

import org.apache.commons.lang.StringUtils;
import org.dspace.app.sherpa.v2.SHERPAJournal;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class SherpaExtraISSNmetadataGenerator implements SherpaExtraMetadataGenerator {

    private String relatedInputFormMetadata;

    @Override
    public Map<String, String> build(SHERPAJournal journal) {
        Map<String, String> extras = new HashMap<String, String>();
        List<String> issns = journal.getIssns();
        String value = issns.isEmpty() ? StringUtils.EMPTY : issns.get(0);
        extras.put("data-" + this.relatedInputFormMetadata, value);
        extras.put(this.relatedInputFormMetadata, value);
        return extras;
    }

    public void setRelatedInputFormMetadata(String relatedInputFormMetadata) {
        this.relatedInputFormMetadata = relatedInputFormMetadata;
    }

}