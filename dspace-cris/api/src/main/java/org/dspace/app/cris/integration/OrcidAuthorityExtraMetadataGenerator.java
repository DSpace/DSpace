package org.dspace.app.cris.integration;

import java.util.Map;

import org.dspace.authority.orcid.OrcidService;

public interface OrcidAuthorityExtraMetadataGenerator
{

    public Map<String, String> build(OrcidService source, String rp);

}
