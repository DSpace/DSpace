/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.configuration.RelationPreferenceConfiguration;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedField;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowseItem;
import org.dspace.browse.BrowserScope;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.authority.AuthorityDAO;
import org.dspace.content.authority.AuthorityDAOFactory;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.PluginManager;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.utils.DSpace;

/**
 * Utility class for performing Item to ReseacherPage binding
 * 
 * @author cilea
 * 
 */
public class BindItemToRP
{
    /** the logger */
    private static Logger log = Logger.getLogger(BindItemToRP.class);

    /**
     * the name of the browse index where lookup for potential matches.
     * Configured in the dspace.cfg with the property
     * <code>researcherpage.browseindex</code>
     */
    private static final String researcherPotentialMatchLookupBrowserIndex = ConfigurationManager
            .getProperty(CrisConstants.CFG_MODULE, "researcherpage.browseindex");

    private RelationPreferenceService relationPreferenceService;

    public static int automaticClaim(Context context, ResearcherPage rp)
            throws SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();

        DSpace dspace = new DSpace();
        ApplicationService applicationService = dspace.getServiceManager()
                .getServiceByName("applicationService",
                        ApplicationService.class);

        RelationPreferenceService relationPreferenceService = dspace
                .getServiceManager()
                .getServiceByName(
                        "org.dspace.app.cris.service.RelationPreferenceService",
                        RelationPreferenceService.class);

        List<RelationPreference> rejected = new ArrayList<RelationPreference>();
        for (RelationPreferenceConfiguration configuration : relationPreferenceService
                .getConfigurationService().getList())
        {
            if (configuration.getRelationConfiguration().getRelationClass().equals(Item.class))
            {
                rejected = applicationService
                        .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                rp.getUuid(), configuration.getRelationConfiguration().getRelationName(),
                                RelationPreference.UNLINKED);
            }
        }

        EPerson eperson = EPerson.find(context, rp.getEpersonID());
        ItemIterator items = Item.findBySubmitter(context, eperson);
        List<MetadataField> mfs = metadataFieldWithAuthorityRP(context);
        int count = 0;
        while (items.hasNext())
        {
            Item item = items.next();
            if (rejected == null || !rejected.contains(item.getID()))
            {
                boolean found = false;
                for (MetadataField mf : mfs)
                {
                    String schema = MetadataSchema.find(context,
                            mf.getSchemaID()).getName();
                    String element = mf.getElement();
                    String qualifier = mf.getQualifier();
                    DCValue[] values = item.getMetadata(schema, element,
                            qualifier, Item.ANY);
                    item.clearMetadata(schema, element, qualifier, Item.ANY);

                    for (DCValue val : values)
                    {
                        if (val.authority == null
                                && val.value != null
                                && StringUtils.containsIgnoreCase(val.value,
                                        eperson.getLastName().trim()))
                        {
                            val.authority = ResearcherPageUtils
                                    .getPersistentIdentifier(rp);
                            val.confidence = Choices.CF_ACCEPTED;
                            found = true;
                        }
                        item.addMetadata(schema, element, qualifier,
                                val.language, val.value, val.authority,
                                val.confidence);
                    }
                }
                if (found)
                {
                    item.update();
                    count++;
                }
            }
        }
        context.restoreAuthSystemState();
        return count;
    }

    public static int countPotentialMatch(Context context, ResearcherPage rp)
            throws SQLException, AuthorizeException, IOException
    {
        return getPotentialMatch(context, rp).size();
    }

    public static long countPendingMatch(Context context, ResearcherPage rp)
            throws SQLException
    {
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
        return dao.countIssuedItemsByAuthorityValueInAuthority(
                RPAuthority.RP_AUTHORITY_NAME,
                ResearcherPageUtils.getPersistentIdentifier(rp));
    }

    public static Set<Integer> getPotentialMatch(Context context,
            ResearcherPage researcher) throws SQLException, AuthorizeException,
            IOException
    {
        Set<Integer> invalidIds = new HashSet<Integer>();
        ItemIterator iter = null;
        try
        {
            iter = getPendingMatch(context, researcher);
            while (iter.hasNext())
            {
                invalidIds.add(iter.nextID());
            }
        }
        finally
        {
            if (iter != null)
                iter.close();
        }

        DSpace dspace = new DSpace();
        ApplicationService applicationService = dspace.getServiceManager()
                .getServiceByName("applicationService",
                        ApplicationService.class);
        RelationPreferenceService relationPreferenceService = dspace
                .getServiceManager()
                .getServiceByName(
                        "org.dspace.app.cris.service.RelationPreferenceService",
                        RelationPreferenceService.class);

        List<RelationPreference> rejected = new ArrayList<RelationPreference>();
        for (RelationPreferenceConfiguration configuration : relationPreferenceService
                .getConfigurationService().getList())
        {
            if (configuration.getRelationConfiguration().getRelationClass().equals(Item.class))
            {
                rejected = applicationService
                        .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                researcher.getUuid(), configuration.getRelationConfiguration().getRelationName(),
                                RelationPreference.UNLINKED);
            }
        }

        for (RelationPreference relationPreference : rejected)
        {
            invalidIds.add(relationPreference.getItemID());
        }
        List<NameResearcherPage> names = new LinkedList<NameResearcherPage>();

        String authority = researcher.getCrisID();
        int id = researcher.getId();
        NameResearcherPage name = new NameResearcherPage(
                researcher.getFullName(), authority, id, invalidIds);
        names.add(name);
        RestrictedField field = researcher.getPreferredName();
        if (field != null && field.getValue() != null
                && !field.getValue().isEmpty())
        {
            NameResearcherPage name_1 = new NameResearcherPage(
                    field.getValue(), authority, id, invalidIds);
            names.add(name_1);
        }
        field = researcher.getTranslatedName();
        if (field != null && field.getValue() != null
                && !field.getValue().isEmpty())
        {
            NameResearcherPage name_2 = new NameResearcherPage(
                    field.getValue(), authority, id, invalidIds);
            names.add(name_2);
        }
        for (RestrictedField r : researcher.getVariants())
        {
            if (r != null && r.getValue() != null && !r.getValue().isEmpty())
            {
                NameResearcherPage name_3 = new NameResearcherPage(
                        r.getValue(), authority, id, invalidIds);
                names.add(name_3);
            }
        }

        Set<Integer> result = new HashSet<Integer>();
        try
        {
            BrowseIndex bi = BrowseIndex
                    .getBrowseIndex(researcherPotentialMatchLookupBrowserIndex);
            // now start up a browse engine and get it to do the work for us
            BrowseEngine be = new BrowseEngine(context);
            int count = 1;

            for (NameResearcherPage tempName : names)
            {
                log.debug("work on " + tempName.getName() + " with identifier "
                        + tempName.getPersistentIdentifier() + " (" + count
                        + " of " + names.size() + ")");
                // set up a BrowseScope and start loading the values into it
                BrowserScope scope = new BrowserScope(context);
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
        catch (BrowseException be)
        {
            log.error(LogManager.getHeader(context, "getPotentialMatch",
                    "researcher=" + researcher.getCrisID()), be);
        }
        return result;
    }

    private static List<MetadataField> metadataFieldWithAuthorityRP(
            Context context) throws SQLException
    {
        // find all metadata with authority support
        MetadataField[] fields = MetadataField.findAll(context);
        List<MetadataField> fieldsWithAuthoritySupport = new LinkedList<MetadataField>();
        for (MetadataField mf : fields)
        {
            String schema = (MetadataSchema.find(context, mf.getSchemaID()))
                    .getName();
            String mdstring = schema
                    + "."
                    + mf.getElement()
                    + (mf.getQualifier() == null ? "" : "." + mf.getQualifier());
            String choicesPlugin = ConfigurationManager
                    .getProperty("choices.plugin." + mdstring);
            if (choicesPlugin != null)
            {
                choicesPlugin = choicesPlugin.trim();
            }
            if ((RPAuthority.RP_AUTHORITY_NAME.equals(choicesPlugin)))
            {
                fieldsWithAuthoritySupport.add(mf);
            }
        }
        return fieldsWithAuthoritySupport;
    }

    public static ItemIterator getPendingMatch(Context context,
            ResearcherPage rp) throws SQLException, AuthorizeException,
            IOException
    {
        AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
        return dao.findIssuedByAuthorityValueInAuthority(
                RPAuthority.RP_AUTHORITY_NAME,
                ResearcherPageUtils.getPersistentIdentifier(rp));
    }

    /**
     * Search potential matches for all the ResearcherPage supplied. The
     * algorithm search for any researcher page and any researcher's name
     * (regardless the visibility attribute) all the items published in DSpace
     * using the Browse System (@link
     * using the Browse System (@link #researcherPotentialMatchLookupBrowserIndex}, if a match is found
     * and there is not an existent authority key for the metadata then the rp
     * identifier of the matching researcher page is used as authority key and a
     * confidence value is attributed as follow:
     * <ul>
     * <li>{@link Choices.CF_UNCERTAIN} if there is only a potential matching
     * researcher page</li>
     * <li>{@link Choices.CF_AMBIGUOUS} if there are more than one potential
     * matching reseacher pages</li>
     * </ul>
     * 
     * @param rps
     *            the list of ResearcherPage
     * @param applicationService
     *            the ApplicationService
     * 
     * @see #researcherPotentialMatchLookupBrowserIndex
     * @see Choices#CF_UNCERTAIN
     * @see Choices#CF_AMBIGUOUS
     * 
     */
    public static void work(List<ResearcherPage> rps,
            RelationPreferenceService relationPreferenceService)
    {
        log.debug("Working...building names list");
        List<NameResearcherPage> names = new LinkedList<NameResearcherPage>();
        for (ResearcherPage researcher : rps)
        {
            Set<Integer> invalidIds = new HashSet<Integer>();
           
            List<RelationPreference> rejected = new ArrayList<RelationPreference>();
            for (RelationPreferenceConfiguration configuration : relationPreferenceService
                    .getConfigurationService().getList())
            {
                if (configuration.getRelationConfiguration().getRelationClass().equals(Item.class))
                {
                    rejected = relationPreferenceService
                            .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                    researcher.getUuid(), configuration.getRelationConfiguration().getRelationName(),
                                    RelationPreference.UNLINKED);
                }
            }
            
            for (RelationPreference relationPreference : rejected)
            {
                invalidIds.add(relationPreference.getItemID());
            }
            String authority = researcher.getCrisID();
            int id = researcher.getId();

            NameResearcherPage name = new NameResearcherPage(
                    researcher.getFullName(), authority, id, invalidIds);
            names.add(name);
            RestrictedField field = researcher.getPreferredName();
            if (field != null && field.getValue() != null
                    && !field.getValue().isEmpty())
            {
                NameResearcherPage name_1 = new NameResearcherPage(
                        field.getValue(), authority, id, invalidIds);
                names.add(name_1);
            }
            field = researcher.getTranslatedName();
            if (field != null && field.getValue() != null
                    && !field.getValue().isEmpty())
            {
                NameResearcherPage name_2 = new NameResearcherPage(
                        field.getValue(), authority, id, invalidIds);
                names.add(name_2);
            }
            for (RestrictedField r : researcher.getVariants())
            {
                if (r != null && r.getValue() != null
                        && !r.getValue().isEmpty())
                {
                    NameResearcherPage name_3 = new NameResearcherPage(
                            r.getValue(), authority, id, invalidIds);
                    names.add(name_3);
                }
            }

        }
        log.debug("...DONE building names list size " + names.size());
        log.debug("Create DSpace context and use browse indexing");
        Context context = null;
        try
        {
            context = new Context();
            context.setIgnoreAuthorization(true);
            
            List<MetadataField> fieldsWithAuthoritySupport = metadataFieldWithAuthorityRP(context);
            
            BrowseIndex bi = BrowseIndex
                    .getBrowseIndex(researcherPotentialMatchLookupBrowserIndex);
            // now start up a browse engine and get it to do the work for us
            BrowseEngine be = new BrowseEngine(context);
            int count = 1;
            for (NameResearcherPage tempName : names)
            {
                log.info("work on " + tempName.getName() + " with identifier "
                        + tempName.getPersistentIdentifier() + " (" + count
                        + " of " + names.size() + ")");
                // set up a BrowseScope and start loading the values into it
                BrowserScope scope = new BrowserScope(context);
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

                BrowseInfo binfo = be.browse(scope);
                log.info("Find " + binfo.getResultCount()
                        + "item(s) in browsing...");
                bindItemsToRP(relationPreferenceService, context,
                        fieldsWithAuthoritySupport, tempName,
                        binfo.getItemResults(context));
                count++;
            }

        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

    }

    public static void bindItemsToRP(RelationPreferenceService relationPreferenceService,
            Context context, ResearcherPage researcher, Item[] items)
            throws SQLException, BrowseException, AuthorizeException
    {
        String authority = researcher.getCrisID();
        int id = researcher.getId();
        List<NameResearcherPage> names = new LinkedList<NameResearcherPage>();
        Set<Integer> invalidIds = new HashSet<Integer>();
      
        List<RelationPreference> rejected = new ArrayList<RelationPreference>();
        for (RelationPreferenceConfiguration configuration : relationPreferenceService
                .getConfigurationService().getList())
        {
            if (configuration.getRelationConfiguration().getRelationClass().equals(Item.class))
            {
                rejected = relationPreferenceService
                        .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                researcher.getUuid(), configuration.getRelationConfiguration().getRelationName(),
                                RelationPreference.UNLINKED);
            }
        }
        
        for (RelationPreference relationPreference : rejected)
        {
            invalidIds.add(relationPreference.getItemID());
        }
        NameResearcherPage name = new NameResearcherPage(
                researcher.getFullName(), authority, id, invalidIds);
        names.add(name);
        RestrictedField field = researcher.getPreferredName();
        if (field != null && field.getValue() != null
                && !field.getValue().isEmpty())
        {
            NameResearcherPage name_1 = new NameResearcherPage(
                    field.getValue(), authority, id, invalidIds);
            names.add(name_1);
        }
        field = researcher.getTranslatedName();
        if (field != null && field.getValue() != null
                && !field.getValue().isEmpty())
        {
            NameResearcherPage name_2 = new NameResearcherPage(
                    field.getValue(), authority, id, invalidIds);
            names.add(name_2);
        }
        for (RestrictedField r : researcher.getVariants())
        {
            if (r != null && r.getValue() != null && !r.getValue().isEmpty())
            {
                NameResearcherPage name_3 = new NameResearcherPage(
                        r.getValue(), authority, id, invalidIds);
                names.add(name_3);
            }
        }

        List<MetadataField> fieldsWithAuthoritySupport = metadataFieldWithAuthorityRP(context);
        for (NameResearcherPage tmpname : names)
        {
            bindItemsToRP(relationPreferenceService, context,
                    fieldsWithAuthoritySupport, tmpname, items);
        }
    }

    private static void bindItemsToRP(RelationPreferenceService relationPreferenceService,
            Context context, List<MetadataField> fieldsWithAuthoritySupport,
            NameResearcherPage tempName, Item[] items) throws BrowseException,
            SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();
        Map<String, Integer> cacheCount = new HashMap<String, Integer>();
        for (Item item : items)
                {
                    if (tempName.getRejectItems() != null
                            && tempName.getRejectItems().contains(item.getID()))
                    {
                log.warn("Item has been reject for this authority - itemID "
                                        + item.getID());
                    }
                    else
                    {
                        boolean modified = false;
                        

                        DCValue[] values = null;
                        for (MetadataField md : fieldsWithAuthoritySupport)
                        {
                    String schema = (MetadataSchema.find(context,
                            md.getSchemaID())).getName();

                            values = item.getMetadata(schema, md.getElement(),
                                    md.getQualifier(), Item.ANY);
                    item.clearMetadata(schema, md.getElement(),
                            md.getQualifier(), Item.ANY);
                            for (DCValue value : values)
                            {

                        int matches = 0;

                                if (value.authority == null
                                && (value.value.equals(tempName.getName()) || value.value
                                        .startsWith(tempName.getName() + ";")))
                                {
                            matches = countNamesMatching(cacheCount,
                                    tempName.getName());
                            item.addMetadata(
                                    value.schema,
                                    value.element,
                                    value.qualifier,
                                    value.language,
                                                    tempName.getName(),
                                            tempName.getPersistentIdentifier(),
                                    matches >= 1 ? Choices.CF_AMBIGUOUS
                                            : matches == 1 ? Choices.CF_UNCERTAIN
                                                    : Choices.CF_NOTFOUND);
                                    modified = true;
                                }
                                else
                                {
                            item.addMetadata(value.schema, value.element,
                                    value.qualifier, value.language,
                                    value.value, value.authority,
                                    value.confidence);
                                }
                            }
                            values = null;
                        }
                        if (modified)
                        {
                    log.debug("Update item with id " + item.getID());
                            item.update();
                        }
                        context.commit();
                        context.clearCache();
                    }
                }
        context.restoreAuthSystemState();
    }

    private static int countNamesMatching(Map<String, Integer> cacheCount,
            String name)
    {
        if (cacheCount.containsKey(name))
        {
            return cacheCount.get(name);
        }
        ChoiceAuthority ca = (ChoiceAuthority) PluginManager.getNamedPlugin(
                ChoiceAuthority.class, RPAuthority.RP_AUTHORITY_NAME);
        Choices choices = ca.getBestMatch(null, name, 0, null);
        cacheCount.put(name, choices.total);
        return choices.total;
    }

    private static void generatePotentialMatches(Context context,
            ResearcherPage researcher) throws SQLException, AuthorizeException,
            IOException
    {
        Set<Integer> ids = getPotentialMatch(context, researcher);
        DatabaseManager.updateQuery(context,
                "delete from potentialmatches where rp like ?",
                researcher.getCrisID());
        for (Integer id : ids)
        {
            TableRow pmTableRow = DatabaseManager.create(context,
                    "potentialmatches");
            pmTableRow.setColumn("rp", researcher.getCrisID());
            pmTableRow.setColumn("item_id", id);
            DatabaseManager.update(context, pmTableRow);
        }
        context.commit();
    }

    public static void generatePotentialMatches(
            ApplicationService applicationService, Context context, String rp)
            throws SQLException, AuthorizeException, IOException
    {
        ResearcherPage researcher = applicationService
                .getResearcherByAuthorityKey(rp);
        if (researcher == null)
        {
            return;
            }

        generatePotentialMatches(context, researcher);
    }

    public static void generatePotentialMatches(ResearcherPage researcher)
    {
        Context context = null;
        try
        {
            context = new Context();
            generatePotentialMatches(context, researcher);
            context.complete();
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
                context.abort();
            }

    }
}

/**
 * Support class to build the full list of names to process in the BindItemToRP
 * work method
 * 
 * @author cilea
 * 
 */
class NameResearcherPage
{
    /** the name form to lookup for */
    private String name;

    /** the rp identifier */
    private String persistentIdentifier;

    private int id;

    /** the ids of previous rejected matches */
    private Set<Integer> rejectItems;

    public NameResearcherPage(String name, String authority, int id,
            Set<Integer> rejectItems)
    {
        this.name = name;
        this.persistentIdentifier = authority;
        this.id = id;
        this.rejectItems = rejectItems;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getPersistentIdentifier()
    {
        return persistentIdentifier;
    }

    public void setPersistentIdentifier(String persistentIdentifier)
    {
        this.persistentIdentifier = persistentIdentifier;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public Set<Integer> getRejectItems()
    {
        return rejectItems;
    }

    public void setRejectItems(Set<Integer> rejectItems)
    {
        this.rejectItems = rejectItems;
    }

}
