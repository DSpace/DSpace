package org.dspace.app.cris.integration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.dspace.authority.orcid.OrcidAccessToken;
import org.dspace.authority.orcid.OrcidService;
import org.orcid.jaxb.model.record_v2.Employment;

public class OrcidSingleEmploymentAuthorityMetadataGenerator
        implements OrcidAuthorityExtraMetadataGenerator
{
    
    private String relatedInputformMetadata = "dc_contributor_department";
    
    @Override
    public Map<String, String> build(OrcidService source, String value)
    {
        Map<String, String> extras = new HashMap<String, String>();
        
        OrcidAccessToken token = null;
        try
        {
            token = source.getMemberSearchToken();
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e.getMessage(), e);
        }
        String access_token = null;
        if (token != null)
        {
            access_token = token.getAccess_token();
        }
        
        Employment employment = source.getEmployment(value, access_token, null);
        if(employment != null) {
            extras.put("data-" + getRelatedInputformMetadata(), employment.getOrganization().getName());    
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
