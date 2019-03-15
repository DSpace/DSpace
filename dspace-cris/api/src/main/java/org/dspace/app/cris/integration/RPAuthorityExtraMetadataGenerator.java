package org.dspace.app.cris.integration;

import java.util.List;
import java.util.Map;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.content.authority.Choice;

/**
 * 
 * Interface to manage simple/aggregation for extra values on authority 
 * 
 * @author Pascarelli Luigi Andrea
 *
 */
public interface RPAuthorityExtraMetadataGenerator
{

    public Map<String, String> build(ResearcherPage rp);
    
    public List<Choice> buildAggregate(ResearcherPage rp);

}
