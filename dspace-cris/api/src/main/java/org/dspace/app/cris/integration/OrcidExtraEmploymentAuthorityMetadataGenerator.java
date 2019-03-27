package org.dspace.app.cris.integration;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.dspace.authority.AuthorityValue;
import org.dspace.authority.orcid.OrcidAccessToken;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.content.authority.Choice;
import org.orcid.jaxb.model.record_v2.Employment;
import org.orcid.jaxb.model.record_v2.EmploymentSummary;
import org.orcid.jaxb.model.record_v2.Employments;

/**
 * 
 * Retrieve employment to fill "extra" authority value 
 * 
 * @author Pascarelli Luigi Andrea
 *
 */
public class OrcidExtraEmploymentAuthorityMetadataGenerator
        implements OrcidAuthorityExtraMetadataGenerator
{

    private String relatedInputformMetadata = "dc_contributor_department";
    
    //use with aggregate mode
    private boolean singleResultOnAggregate = true;
    
    @Override
    public Map<String, String> build(OrcidService source, String value)
    {
        Map<String, String> extras = new HashMap<String, String>();
        
        String access_token = getAccessToken(source);
        
        Employment employment = source.getEmployment(value, access_token, null);
        if(employment != null) {
            extras.put("data-" + getRelatedInputformMetadata(), employment.getOrganization().getName());    
        }
        else {
            //manage value to empty html element
            extras.put("data-" + getRelatedInputformMetadata(), "");
        }
        return extras;
    }

    private String getAccessToken(OrcidService source)
    {
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
        return access_token;
    }

    public String getRelatedInputformMetadata()
    {
        return relatedInputformMetadata;
    }

    public void setRelatedInputformMetadata(String relatedInputformMetadata)
    {
        this.relatedInputformMetadata = relatedInputformMetadata;
    }

    @Override
    public List<Choice> buildAggregate(OrcidService source, AuthorityValue val)
    {
        List<Choice> choiceList = new LinkedList<Choice>();
        if (isSingleResultOnAggregate())
        {            
            choiceList.add(new Choice(val.generateString(), val.getValue(), val.getValue(), build(source, val.getValue())));
        }
        else
        {
            String access_token = getAccessToken(source);
            
            Employments employments = source.getEmployments(val.getValue(), access_token);
            for (EmploymentSummary employment : employments.getEmploymentSummary())
            {
                Map<String, String> extras = new HashMap<String, String>();
                if(employment != null) {
                    extras.put("data-" + getRelatedInputformMetadata(), employment.getOrganization().getName());    
                }
                else {
                    //manage value to empty html element
                    extras.put("data-" + getRelatedInputformMetadata(), "");
                }
                choiceList.add(new Choice(val.generateString(), val.getValue(), val.getValue(), extras));
            }
            // manage value to empty html element
            if (choiceList.isEmpty())
            {
                Map<String, String> extras = new HashMap<String, String>();
                extras.put("data-" + getRelatedInputformMetadata(), "");
                choiceList.add(new Choice(val.generateString(), val.getValue(), val.getValue(), extras));
            }
        }
        return choiceList;        
    }

    public boolean isSingleResultOnAggregate()
    {
        return singleResultOnAggregate;
    }

    public void setSingleResultOnAggregate(boolean singleResultOnAggregate)
    {
        this.singleResultOnAggregate = singleResultOnAggregate;
    }
}
