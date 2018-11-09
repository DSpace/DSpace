/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowseItem;
import org.dspace.browse.BrowserScope;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;

public class CrisRetrievePotentialMatchPlugin implements
        IRetrievePotentialMatchPlugin
{

    /** the logger */
    private static Logger log = Logger
            .getLogger(CrisRetrievePotentialMatchPlugin.class);

    @Override
    public Set<Integer> retrieve(Context context, Set<Integer> invalidIds,
            ResearcherPage researcher)
    {


        String authority = researcher.getCrisID();

        List<NameResearcherPage> names = ResearcherPageUtils.getAllVariantsName(invalidIds,
                researcher);

        Set<Integer> result = new HashSet<Integer>();
        try
        {
            String researcherPotentialMatchLookupBrowserIndex = ConfigurationManager
                    .getProperty(CrisConstants.CFG_MODULE, "researcherpage.browseindex");
            BrowseIndex bi = BrowseIndex
                    .getBrowseIndex(researcherPotentialMatchLookupBrowserIndex);
            boolean isMultilanguage = new DSpace()
            .getConfigurationService()
            .getPropertyAsType(
                    "discovery.browse.authority.multilanguage."
                            + bi.getName(),
                    new DSpace()
                            .getConfigurationService()
                            .getPropertyAsType(
                                    "discovery.browse.authority.multilanguage",
                                    new Boolean(false)),
                    false);
            
            int count = 1;

            for (NameResearcherPage tempName : names)
            {
                log.debug("work on " + tempName.getName() + " with identifier "
                        + tempName.getPersistentIdentifier() + " (" + count
                        + " of " + names.size() + ")");
                // set up a BrowseScope and start loading the values into it
                BrowserScope scope = new BrowserScope(context);
                scope.setUserLocale(context.getCurrentLocale().getLanguage());
                scope.setBrowseIndex(bi);
                // scope.setOrder(order);
                scope.setFilterValue(tempName.getName());
                // scope.setFilterValueLang(valueLang);
                // scope.setJumpToItem(focus);
                // scope.setJumpToValue(valueFocus);
                // scope.setJumpToValueLang(valueFocusLang);
                // scope.setStartsWith(startsWith);
                // scope.setOffset(offset);
                scope.setResultsPerPage(Integer.MAX_VALUE);
                // scope.setSortBy(sortBy);
                scope.setBrowseLevel(1);
                // scope.setEtAl(etAl);
                
                // now start up a browse engine and get it to do the work for us
                BrowseEngine be = new BrowseEngine(context, isMultilanguage? 
                        scope.getUserLocale():null);

                BrowseInfo binfo = be.browse(scope);
                log.debug("Find " + binfo.getResultCount()
                        + "item(s) in browsing...");
                for (BrowseItem bitem : binfo.getBrowseItemResults())
                {
                    if (!invalidIds.contains(bitem.getID()))
                    {
                        result.add(bitem.getID());
                    }
                }
            }
        }
        catch (BrowseException e)
        {
            log.error(LogManager.getHeader(context, "getPotentialMatch",
                    "researcher=" + authority), e);
        }

        return result;
    }

 

    @Override
    public Map<NameResearcherPage, Item[]> retrieveGroupByName(Context context,
            Map<String, Set<Integer>> mapInvalids, List<ResearcherPage> rps, boolean partialMatch)
    {
        return retrieveGroupByNameExceptAuthority(context, mapInvalids, rps,
                partialMatch, false);
    }



    @Override
    public Map<NameResearcherPage, Item[]> retrieveGroupByNameExceptAuthority(
            Context context, Map<String, Set<Integer>> mapInvalids,
            List<ResearcherPage> rps, boolean partialMatch, boolean excludeMatchForAuthority)
    {
        
        Map<NameResearcherPage, Item[]> result = new HashMap<NameResearcherPage, Item[]>();

        for (ResearcherPage researcher : rps)
        {
            String authority = researcher.getCrisID();
            BrowseIndex bi;
            try
            {
                String researcherPotentialMatchLookupBrowserIndex = ConfigurationManager
                        .getProperty(CrisConstants.CFG_MODULE, "researcherpage.browseindex");
                bi = BrowseIndex
                        .getBrowseIndex(researcherPotentialMatchLookupBrowserIndex);
                boolean isMultilanguage = new DSpace()
                        .getConfigurationService()
                        .getPropertyAsType(
                                "discovery.browse.authority.multilanguage."
                                        + bi.getName(),
                                new DSpace()
                                        .getConfigurationService()
                                        .getPropertyAsType(
                                                "discovery.browse.authority.multilanguage",
                                                new Boolean(false)),
                                false);
                int count = 1;
                List<NameResearcherPage> names = ResearcherPageUtils.getAllVariantsName(mapInvalids.get(authority),
                        researcher);
                for (NameResearcherPage tempName : names)
                {
                    log.info("work on " + tempName.getName()
                            + " with identifier "
                            + tempName.getPersistentIdentifier() + " (" + count
                            + " of " + names.size() + ")");
                    // set up a BrowseScope and start loading the values into it
                    BrowserScope scope = new BrowserScope(context);
                    scope.setUserLocale(context.getCurrentLocale().getLanguage());
                    scope.setBrowseIndex(bi);
                    // scope.setOrder(order);
                    if(excludeMatchForAuthority) {
                        scope.setAuthorityValue(authority);
                    }
                    scope.setFilterValue(tempName.getName());
                    scope.setFilterValuePartial(partialMatch);
                    
                    String handleCommunity = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "retrievepotentialmatch.filter.bycommunity");
                    if(StringUtils.isNotBlank(handleCommunity)) {
                        Community community = (Community)HandleManager.resolveToObject(context, handleCommunity);
                        scope.setCommunity(community);
                    }
                    String handleCollection = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "retrievepotentialmatch.filter.bycollection");
                    if(StringUtils.isNotBlank(handleCollection)) {
                        Collection collection = (Collection)HandleManager.resolveToObject(context, handleCollection);
                        scope.setCollection(collection);
                    }
                    // scope.setFilterValueLang(valueLang);
                    // scope.setJumpToItem(focus);
                    // scope.setJumpToValue(valueFocus);
                    // scope.setJumpToValueLang(valueFocusLang);
                    // scope.setStartsWith(startsWith);
                    // scope.setOffset(offset);
                    scope.setResultsPerPage(Integer.MAX_VALUE);
                    // scope.setSortBy(sortBy);
                    scope.setBrowseLevel(1);
                    // scope.setEtAl(etAl);

                    // now start up a browse engine and get it to do the work for us
                    BrowseEngine be = new BrowseEngine(context, isMultilanguage? 
                            scope.getUserLocale():null);
                    
                    BrowseInfo binfo = be.browse(scope);
                    log.info("Find " + binfo.getResultCount()
                            + "item(s) in browsing...");
                    result.put(tempName, binfo.getItemResults(context));
                    count++;
                }
            }
            catch (BrowseException | IllegalStateException | SQLException e)
            {
                log.error(LogManager.getHeader(context, "getPotentialMatch",
                        "researcher=" + authority), e);
            }
        }
        return result;
    }

}
