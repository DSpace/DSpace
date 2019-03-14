package org.dspace.app.cris.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedFieldWithLock;

public class RPSingleOrgUnitAuthorityMetadataGenerator
        implements RPAuthorityExtraMetadataGenerator
{
    
    private String relatedInputformMetadata = "dc_contributor_department";
    
    @Override
    public Map<String, String> build(ResearcherPage rp)
    {
        Map<String, String> extras = new HashMap<String, String>();
        List<RestrictedFieldWithLock> metadatas = rp.getOrgUnit();
        for(RestrictedFieldWithLock mm : metadatas) {
            if(StringUtils.isNotBlank(mm.getAuthority())) {
                extras.put("data-" + getRelatedInputformMetadata(), mm.getValue()+"::"+mm.getAuthority());
            }
            else {
                extras.put("data-" + getRelatedInputformMetadata(), mm.getValue());
            }
            break;
        }
        //manage value to empty html element
        if(metadatas==null || metadatas.isEmpty()) {
            extras.put("data-" + getRelatedInputformMetadata(), "");
        }
        return extras;
    }

    public String getRelatedInputformMetadata()
    {
        return relatedInputformMetadata;
    }

    public void setRelatedInputformMetadata(String relatedInputformMetadata)
    {
        this.relatedInputformMetadata = relatedInputformMetadata;
    }
}
