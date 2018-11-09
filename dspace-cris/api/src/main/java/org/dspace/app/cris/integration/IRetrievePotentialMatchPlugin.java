/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
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
    
    Map<NameResearcherPage, Item[]> retrieveGroupByName(Context context, Map<String, Set<Integer>> mapInvalids, List<ResearcherPage> rps, boolean partialMatch);
    
    Map<NameResearcherPage, Item[]> retrieveGroupByNameExceptAuthority(Context context, Map<String, Set<Integer>> mapInvalids, List<ResearcherPage> rps, boolean partialMatch, boolean excludeMatchForAuthority);
}
