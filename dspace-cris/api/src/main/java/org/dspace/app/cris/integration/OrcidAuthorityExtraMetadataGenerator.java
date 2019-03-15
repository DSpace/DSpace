package org.dspace.app.cris.integration;

import java.util.List;
import java.util.Map;

import org.dspace.authority.AuthorityValue;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.content.authority.Choice;

/**
 * 
 * Interface to manage simple/aggregation for extra values on authority 
 * 
 * @author Pascarelli Luigi Andrea
 *
 */
public interface OrcidAuthorityExtraMetadataGenerator
{
    public Map<String, String> build(OrcidService source, String rp);

    public List<Choice> buildAggregate(OrcidService source, AuthorityValue value);
}
