package org.dspace.app.cris.integration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.content.Item;
import org.dspace.core.Context;

public interface IRetrievePotentialMatchPlugin
{
    Set<Integer> retrieve(Context context, Set<Integer> invalidIds, ResearcherPage rp);
    
    Map<NameResearcherPage, Item[]> retrieveGroupByName(Context context, Map<String, Set<Integer>> mapInvalids, List<ResearcherPage> rps);
}
