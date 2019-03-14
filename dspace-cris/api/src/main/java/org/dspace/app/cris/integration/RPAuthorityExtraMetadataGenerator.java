package org.dspace.app.cris.integration;

import java.util.Map;

import org.dspace.app.cris.model.ResearcherPage;

public interface RPAuthorityExtraMetadataGenerator
{

    public Map<String, String> build(ResearcherPage rp);

}
