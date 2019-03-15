package org.dspace.app.cris.integration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedFieldWithLock;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.authority.Choice;

/**
 * 
 * Dedicated generator to work on orgunit metadata {@link ResearcherPage#getOrgUnit()}
 * 
 * @author Pascarelli Luigi Andrea
 *
 */
public class RPExtraOrgUnitAuthorityMetadataGenerator
        extends RPExtraAuthorityMetadataGenerator
{
    
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
    
    @Override
    public List<Choice> buildAggregate(ResearcherPage rp)
    {
        List<Choice> choiceList = new LinkedList<Choice>();
        if (isSingleResultOnAggregate())
        {
            Map<String, String> extras = new HashMap<String, String>();
            buildSingleExtraByRP(rp, extras);
            choiceList.add(
                    new Choice(ResearcherPageUtils.getPersistentIdentifier(rp),
                            rp.getFullName(),
                            ResearcherPageUtils.getLabel(rp.getFullName(), rp),
                            extras));
        }
        else
        {
            List<RestrictedFieldWithLock> metadatas = rp.getOrgUnit();
            for(RestrictedFieldWithLock mm : metadatas) {
                Map<String, String> extras = new HashMap<String, String>();
                if(StringUtils.isNotBlank(mm.getAuthority())) {
                    extras.put("data-" + getRelatedInputformMetadata(), mm.getValue()+"::"+mm.getAuthority());
                }
                else {
                    extras.put("data-" + getRelatedInputformMetadata(), mm.getValue());
                }
                choiceList.add(new Choice(
                        ResearcherPageUtils.getPersistentIdentifier(rp),
                        rp.getFullName(),
                        ResearcherPageUtils.getLabel(rp.getFullName(), rp),
                        extras));
            }
            // manage value to empty html element
            if (metadatas == null || metadatas.isEmpty())
            {
                Map<String, String> extras = new HashMap<String, String>();
                extras.put("data-" + getRelatedInputformMetadata(), "");
                choiceList.add(new Choice(
                        ResearcherPageUtils.getPersistentIdentifier(rp),
                        rp.getFullName(),
                        ResearcherPageUtils.getLabel(rp.getFullName(), rp),
                        extras));
            }
        }
        return choiceList;
    }

    @Override
    protected void buildSingleExtraByRP(ResearcherPage rp,
            Map<String, String> extras)
    {
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
    }
}
