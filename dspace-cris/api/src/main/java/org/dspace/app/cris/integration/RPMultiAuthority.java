/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedField;
import org.dspace.app.cris.model.VisibilityConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.DCPersonName;
import org.dspace.content.DSpaceObject;
import org.dspace.content.authority.AuthorityVariantsSupport;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.NotificableAuthority;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.utils.DSpace;

/**
 * 
 * Authority to aggregate "extra" value to single choice 
 * 
 * @author Pascarelli Luigi Andrea
 *
 */
public class RPMultiAuthority extends CRISAuthority
        implements AuthorityVariantsSupport, NotificableAuthority
{
    public static final String PUBLICATIONS_RELATION_NAME = "publications";

    /** The logger */
    private static Logger log = Logger.getLogger(RPMultiAuthority.class);

    /** The name as this ChoiceAuthority MUST be configurated */
    public final static String RP_AUTHORITY_NAME = "RPAuthority";

    /** The RPs database service layer */
    private ApplicationService applicationService;

    /** The RPs search service layer */
    private SearchService searchService;

    private ConfigurationService configurationService;

    private RelationPreferenceService relationPreferenceService;

    private List<String> metadataAuth;

    private List<RPAuthorityExtraMetadataGenerator> generators = new DSpace()
            .getServiceManager()
            .getServicesByType(RPAuthorityExtraMetadataGenerator.class);
    
    /**
     * Make sure that the class is fully initialized before use it
     */
    private void init()
    {
        if (applicationService == null && searchService == null)
        {
            DSpace dspace = new DSpace();
            applicationService = dspace.getServiceManager().getServiceByName(
                    "applicationService", ApplicationService.class);
            searchService = dspace.getServiceManager().getServiceByName(
                    "org.dspace.discovery.SearchService", SearchService.class);

            configurationService = dspace.getServiceManager().getServiceByName(
                    "org.dspace.services.ConfigurationService",
                    ConfigurationService.class);

            relationPreferenceService = dspace.getServiceManager()
                    .getServiceByName(
                            "org.dspace.app.cris.service.RelationPreferenceService",
                            RelationPreferenceService.class);
        }
    }

    /**
     * Empty constructor needed by the DSpace plugin facility.
     */
    public RPMultiAuthority()
    {
        // nothing to do, initialization is provided by the IoC Spring Frameword
    }

    /**
     * Return a list of choices performing a lucene query on the RPs names index
     * appending the wildchar to every word in the query (i.e. if the query
     * string is Chan Tse the search will be perfomed with Chan* Tse*). For any
     * matching RP will be returned choices for every variants form.
     * 
     * {@link ChoiceAuthority#getMatches(String, int, int, int, String)}
     * 
     * @param query
     *            the lookup string
     * @param collection
     *            (not used by this Authority)
     * @param locale
     *            (not used by this Authority)
     * @param start
     *            (not used by this Authority)
     * @param limit
     *            (not used by this Authority)
     * @param locale
     *            (not used by this Authority)
     * 
     * @return a Choices of RPs where a name form match the query string
     */
    @Override
    public Choices getMatches(String field, String query, int collection,
            int start, int limit, String locale)
    {

        try
        {
            init();
            ConfigurationService _configurationService = this.configurationService;
            SearchService _searchService = this.searchService;

            Choices choicesResult = null;
            if (query != null && query.length() > 1)
            {
                DCPersonName tmpPersonName = new DCPersonName(
                        query.toLowerCase());

                String luceneQuery = "";
                if (StringUtils.isNotBlank(tmpPersonName.getLastName()))
                {
                    luceneQuery += ClientUtils.escapeQueryChars(
                            tmpPersonName.getLastName().trim())
                            + (StringUtils.isNotBlank(
                                    tmpPersonName.getFirstNames()) ? "" : "*");
                }

                if (StringUtils.isNotBlank(tmpPersonName.getFirstNames()))
                {
                    luceneQuery += (luceneQuery.length() > 0 ? " " : "")
                            + ClientUtils.escapeQueryChars(
                                    tmpPersonName.getFirstNames().trim())
                            + "*";
                }
                luceneQuery = luceneQuery.replaceAll("\\\\ ", " ");
                DiscoverQuery discoverQuery = new DiscoverQuery();
                ResearcherPageUtils.applyCustomFilter(field, discoverQuery,
                        _configurationService);
                discoverQuery.setSortField("crisrp.fullName_sort",
                        SORT_ORDER.asc);
                discoverQuery.setDSpaceObjectFilter(CrisConstants.RP_TYPE_ID);
                String surnameQuery = "{!lucene q.op=AND df=rpsurnames}("
                        + luceneQuery + ") OR ("
                        // no need for a phrase search, the default operator is
                        // now AND and we want to match surnames in any order
                        + luceneQuery.substring(0, luceneQuery.length() - 1)
                        + ")";

                discoverQuery.setQuery(surnameQuery);
                discoverQuery.setMaxResults(ResearcherPageUtils.MAX_RESULTS);

                DiscoverResult result = _searchService.search(null,
                        discoverQuery, true);
                List<Choice> choiceList = new LinkedList<Choice>();
                for (DSpaceObject dso : result.getDspaceObjects())
                {
                    ResearcherPage rp = (ResearcherPage) dso;
                    choiceList.addAll(buildAggregateByExtra(rp));
                }
                
                int surnamesResult = choiceList.size();
                
                if (surnamesResult<ResearcherPageUtils.MAX_RESULTS){
                    int difference = ResearcherPageUtils.MAX_RESULTS - surnamesResult;
                    discoverQuery.setMaxResults(difference);
                    String crisauthoritylookup = "{!lucene q.op=AND df=crisauthoritylookup}("
                                                 + luceneQuery
                                                 + ") OR (\""
                                                 + luceneQuery.substring(0,luceneQuery.length() - 1) + "\")";
                    
                    discoverQuery.setQuery(crisauthoritylookup);
                    String negativeFilters = "-rpsurnames:(" + luceneQuery.substring(0,luceneQuery.length() - 1) + ")";
                    String negativeFiltersStar = "-rpsurnames:(" + luceneQuery + ")";
                    discoverQuery.addFilterQueries(negativeFilters);
                    discoverQuery.addFilterQueries(negativeFiltersStar);
                    result = _searchService.search(null, discoverQuery, true);
                    for (DSpaceObject dso : result.getDspaceObjects())
                    {
                        ResearcherPage rp = (ResearcherPage) dso;
                        choiceList.addAll(buildAggregateByExtra(rp));
                    }
                }
                int foundSize = choiceList.size();
                Choice[] results = new Choice[foundSize];
                results = choiceList.toArray(results);
                choicesResult = new Choices(results, 0, foundSize,
                        Choices.CF_AMBIGUOUS, false, 0);
            }
            else
            {
                choicesResult = new Choices(false);
            }

            return choicesResult;
        }
        catch (Exception e)
        {
            log.error("Error quering the RPAuthority - " + e.getMessage(), e);
            return new Choices(true);
        }
    }

    private List<Choice> buildAggregateByExtra(ResearcherPage rp)
    {
        List<Choice> choiceList = new LinkedList<Choice>();
        if (generators != null)
        {
            for (RPAuthorityExtraMetadataGenerator gg : generators)
            {
                choiceList.addAll(gg.buildAggregate(rp));
            }
        }
        return choiceList;
    }

    protected String generateDisplayValue(String value, ResearcherPage rp)
    {
        return ResearcherPageUtils.getLabel(value, rp);
    }

    /**
     * Return a list of choices performing an exact query on the RP names (full,
     * Chinese, academinc, variants). For any matching RP will be returned
     * choices for every variants form, the default choice will be which that
     * match with the "query" string. This method is used by unattended
     * submssion only, interactive submission will use the
     * {@link RPMultiAuthority#getMatches(String, String, int, int, int, String)}.
     * The confidence value of the returned Choices will be
     * {@link Choices#CF_UNCERTAIN} if there is only a RP that match with the
     * lookup string or {@link Choices#CF_AMBIGUOUS} if there are more RPs.
     * 
     * {@link ChoiceAuthority#getMatches(String, String, int, int, int, String)}
     * 
     * @param field
     *            (not used by this Authority)
     * @param text
     *            the lookup string
     * @param collection
     *            (not used by this Authority)
     * @param locale
     *            (not used by this Authority)
     * @return a Choices of RPs that have an exact string match between a name
     *         forms and the text lookup string
     */
    @Override
    public Choices getBestMatch(String field, String text, int collection,
            String locale)
    {

        try
        {
            init();
            List<Choice> choiceList = new ArrayList<Choice>();
            int totalResult = 0;
            if (text != null && text.length() > 2)
            {
                text = text.replaceAll("\\.", "");
                DiscoverQuery discoverQuery = new DiscoverQuery();
                discoverQuery.setDSpaceObjectFilter(CrisConstants.RP_TYPE_ID);
                ResearcherPageUtils.applyCustomFilter(field, discoverQuery,
                        configurationService);

                discoverQuery
                        .setQuery("{!lucene q.op=AND df=crisauthoritylookup}\""
                                + ClientUtils.escapeQueryChars(text.trim())
                                + "\"");
                discoverQuery.setMaxResults(50);
                DiscoverResult result = searchService.search(null,
                        discoverQuery, true);
                totalResult = (int) result.getTotalSearchResults();
                for (DSpaceObject dso : result.getDspaceObjects())
                {
                    ResearcherPage rp = (ResearcherPage) dso;
                    buildAggregateByExtra(rp);
                }
            }

            Choice[] results = new Choice[choiceList.size()];
            if (choiceList.size() > 0)
            {
                results = choiceList.toArray(results);

                if (totalResult == 1)
                {
                    return new Choices(results, 0, totalResult,
                            Choices.CF_UNCERTAIN, false, 0);
                }
                else
                {
                    return new Choices(results, 0, totalResult,
                            Choices.CF_AMBIGUOUS, false, 0);
                }
            }
            else
            {
                return new Choices(false);
            }
        }
        catch (Exception e)
        {
            log.error("Error quering the CRISAuthority - " + e.getMessage(), e);
            return new Choices(true);
        }
    }

    /**
     * Return a list of all not null "public" forms of the RP name.
     * 
     * @param key
     *            the researcher identifier (i.e. rp00024)
     * @param locale
     *            (not used by this Authority)
     * 
     * @return a list of all not null "public" forms of the RP name.
     */
    public List<String> getVariants(String key, String locale)
    {
        init();
        Integer id = ResearcherPageUtils.getRealPersistentIdentifier(key,
                ResearcherPage.class);
        if (id == null)
        {
            log.error(LogManager.getHeader(null, "getLabel",
                    "invalid key for hkuauthority key " + key));
            return null;
        }
        ResearcherPage rp = applicationService.get(ResearcherPage.class, id);
        List<String> publicNames = new ArrayList<String>();
        publicNames.add(rp.getFullName());
        if (rp.getTranslatedName() != null && rp.getTranslatedName()
                .getVisibility() == VisibilityConstants.PUBLIC)
        {
            String value = rp.getTranslatedName().getValue();
            if (StringUtils.isNotBlank(value))
            {
                publicNames.add(value);
            }
        }

        if (rp.getPreferredName() != null && rp.getPreferredName()
                .getVisibility() == VisibilityConstants.PUBLIC)
        {
            String value = rp.getPreferredName().getValue();
            if (StringUtils.isNotBlank(value))
            {
                publicNames.add(value);
            }
        }

        List<RestrictedField> variants = rp.getVariants();
        if (variants != null)
        {
            for (RestrictedField v : variants)
            {
                if (v.getVisibility() == VisibilityConstants.PUBLIC)
                {
                    String value = v.getValue();
                    if (StringUtils.isNotBlank(value))
                    {
                        publicNames.add(value);
                    }
                }
            }
        }

        return publicNames;
    }

    /**
     * Record the reject of a potential match so to avoid the same proposal to
     * happen in future.
     * 
     * @param itemID
     *            the id of the item that has been rejected
     * @param authorityKey
     *            the researcher identifier (i.e. rp00024)
     */
    public void reject(int itemID, String authorityKey)
    {
        int[] itemIDs = { itemID };
        reject(itemIDs, authorityKey);
    }

    /**
     * Record the reject of a potential match so to avoid the same proposal to
     * happen in future.
     * 
     * @param itemIDs
     *            the id of the items that has been rejected
     * @param authorityKey
     *            the researcher identifier (i.e. rp00024)
     */
    public void reject(int[] itemIDs, String authorityKey)
    {
        init();
        ResearcherPage cris = applicationService
                .getResearcherByAuthorityKey(authorityKey);
        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();
            List<String> list = new ArrayList<String>();
            for (int itemID : itemIDs)
            {
                list.add(String.valueOf(itemID));
                DatabaseManager.updateQuery(context,
                        "delete from potentialmatches where rp like ? and item_id = ?",
                        cris.getCrisID(), itemID);
            }

            String defaultRelation = configurationService.getPropertyAsType(
                    RP_AUTHORITY_NAME + ".relation.name",
                    PUBLICATIONS_RELATION_NAME, true);
            relationPreferenceService.unlink(context, cris, defaultRelation,
                    list);
            context.commit();
            context.restoreAuthSystemState();
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

    }

    @Override
    public int getCRISTargetTypeID()
    {
        return CrisConstants.RP_TYPE_ID;
    }

    @Override
    public Class<ResearcherPage> getCRISTargetClass()
    {
        return ResearcherPage.class;
    }

    public String getPublicPath()
    {
        return "rp";
    }

    @Override
    public void accept(int itemID, String authorityKey, int confidence)
    {
        init();
        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();
            if (confidence != Choices.CF_ACCEPTED)
            {
                if (DatabaseManager.updateQuery(context,
                        "update potentialmatches set pending=1 where rp like ? and item_id = ?",
                        authorityKey, itemID) != 1)
                {
                    TableRow row = DatabaseManager.create(context,
                            "potentialmatches");
                    row.setColumn("pending", true);
                    row.setColumn("rp", authorityKey);
                    row.setColumn("item_id", itemID);
                    DatabaseManager.update(context, row);
                }
            }
            else
            {
                DatabaseManager.updateQuery(context,
                        "delete from potentialmatches where rp like ? and item_id = ?",
                        authorityKey, itemID);
            }
            context.commit();

            ResearcherPage rp = applicationService
                    .getResearcherByAuthorityKey(authorityKey);
            List<String> list = new ArrayList<String>();
            list.add(String.valueOf(itemID));
            String defaultRelation = configurationService.getPropertyAsType(
                    RP_AUTHORITY_NAME + ".relation.name",
                    PUBLICATIONS_RELATION_NAME, true);
            relationPreferenceService.active(context, rp, defaultRelation,
                    list);
            context.restoreAuthSystemState();
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

    }

    @Override
    public ResearcherPage getNewCrisObject()
    {
        return new ResearcherPage();
    }
}
