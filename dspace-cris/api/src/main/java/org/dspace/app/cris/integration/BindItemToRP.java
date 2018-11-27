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

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.configuration.RelationPreferenceConfiguration;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedField;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseException;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.AuthorityDAO;
import org.dspace.content.authority.AuthorityDAOFactory;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
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
    private static final String COMMAND_WORK = "work";
    
    private static final String COMMAND_LIST = "list";

    private static final String COMMAND_LISTEXCLUDEAUTHORITY = "listexcludeauthority";

    /** the logger */
    private static Logger log = Logger.getLogger(BindItemToRP.class);

    public static final String EMAIL_TEMPLATE_NAME = "binditemtorp-alerts";
    
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
            if (configuration.getRelationConfiguration().getRelationClass()
                    .equals(Item.class))
            {
                rejected = applicationService
                        .findRelationsPreferencesByUUIDByRelTypeAndStatus(rp
                                .getUuid(), configuration
                                .getRelationConfiguration().getRelationName(),
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
                    Metadatum[] values = item.getMetadata(schema, element,
                            qualifier, Item.ANY);
                    item.clearMetadata(schema, element, qualifier, Item.ANY);

                    for (Metadatum val : values)
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
            if (configuration.getRelationConfiguration().getRelationClass()
                    .equals(Item.class))
            {
                rejected = applicationService
                        .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                researcher.getUuid(), configuration
                                        .getRelationConfiguration()
                                        .getRelationName(),
                                RelationPreference.UNLINKED);
            }
        }

        for (RelationPreference relationPreference : rejected)
        {
            invalidIds.add(relationPreference.getItemID());
        }

        IRetrievePotentialMatchPlugin plugin = dspace
                .getSingletonService(IRetrievePotentialMatchPlugin.class);
        Set<Integer> result = plugin.retrieve(context, invalidIds, researcher);

        return result;
    }

    public static List<MetadataField> metadataFieldWithAuthorityRP(
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
     * using the Browse System (@link using the Browse System (@link
     * #researcherPotentialMatchLookupBrowserIndex}, if a match is found and
     * there is not an existent authority key for the metadata then the rp
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

        doWorkOrList(COMMAND_WORK, rps, relationPreferenceService);
    }

    public static void bindItemsToRP(
            RelationPreferenceService relationPreferenceService,
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
            if (configuration.getRelationConfiguration().getRelationClass()
                    .equals(Item.class))
            {
                rejected = relationPreferenceService
                        .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                researcher.getUuid(), configuration
                                        .getRelationConfiguration()
                                        .getRelationName(),
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

    private static void bindItemsToRP(
            RelationPreferenceService relationPreferenceService,
            Context context, List<MetadataField> fieldsWithAuthoritySupport,
            NameResearcherPage tempName, Item[] items) throws BrowseException,
            SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();
        Map<String, Integer> cacheCount = new HashMap<String, Integer>();

        Map<String, List<String>> mapOwnerToNewHandleBinded = new HashMap<String, List<String>>();
        boolean sendEmailToOwner = ConfigurationManager.getBooleanProperty("cris","system.script.subscribe.binditemtorp", false);
        
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

                Metadatum[] values = null;
                for (MetadataField md : fieldsWithAuthoritySupport)
                {
                    String schema = (MetadataSchema.find(context,
                            md.getSchemaID())).getName();

                    values = item.getMetadata(schema, md.getElement(),
                            md.getQualifier(), Item.ANY);
                    item.clearMetadata(schema, md.getElement(),
                            md.getQualifier(), Item.ANY);
                    for (Metadatum value : values)
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
                            
							if (sendEmailToOwner) {
								if (mapOwnerToNewHandleBinded.containsKey(tempName.getPersistentIdentifier())) {
									List<String> handles = mapOwnerToNewHandleBinded
											.get(tempName.getPersistentIdentifier());
									handles.add(item.getHandle());
									mapOwnerToNewHandleBinded.put(tempName.getPersistentIdentifier(), handles);
								} else {
									List<String> handles = new ArrayList<String>();
									handles.add(item.getHandle());
									mapOwnerToNewHandleBinded.put(tempName.getPersistentIdentifier(), handles);
								}
							}
                            
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
        
        if(sendEmailToOwner) {
			sendEmail(context, mapOwnerToNewHandleBinded, relationPreferenceService);
        }
        context.restoreAuthSystemState();
    }

	private static void sendEmail(Context context, Map<String, List<String>> mapOwnerToNewHandleBinded,
			RelationPreferenceService relationPreferenceService) {

		org.dspace.core.Email email;
		try {
			email = org.dspace.core.Email
					.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), BindItemToRP.EMAIL_TEMPLATE_NAME));

			ApplicationService applicationService = relationPreferenceService.getApplicationService();
			int i = 0;
			for (String owner : mapOwnerToNewHandleBinded.keySet()) {
				if (i > 0) {
					email.reset();
				}

				ResearcherPage rp = applicationService.getEntityByCrisId(owner, ResearcherPage.class);
				if (rp.getEpersonID() != null) {
					EPerson eperson;
					try {
						eperson = EPerson.find(context, rp.getEpersonID());
						if (eperson != null) {
							email.addRecipient(eperson.getEmail());
							email.addArgument(mapOwnerToNewHandleBinded.get(owner));
							email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/cris/rp/" + owner);
							email.send();
						}
					} catch (SQLException | MessagingException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}

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
        String crisID = researcher.getCrisID();
		deletePotentialMatch(context, crisID);
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

	public static void deletePotentialMatch(Context context, String crisID) throws SQLException {
		DatabaseManager
                .updateQuery(
                        context,
                        "DELETE FROM potentialmatches WHERE rp like ? AND pending IS NULL",
                        crisID);
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
    
    public static Map<NameResearcherPage, Item[]> list(List<ResearcherPage> rps,
            RelationPreferenceService relationPreferenceService)
    {
        return doWorkOrList(COMMAND_LIST, rps, relationPreferenceService);
    }

    public static Map<NameResearcherPage, Item[]> listExcludeAuthority(List<ResearcherPage> rps,
            RelationPreferenceService relationPreferenceService)
    {
        return doWorkOrList(COMMAND_LISTEXCLUDEAUTHORITY, rps, relationPreferenceService);
    }
    
    public static Map<NameResearcherPage, Item[]> doWorkOrList(String command, List<ResearcherPage> rps,
            RelationPreferenceService relationPreferenceService)
    {
        log.debug("Working...building names list");

        List<NameResearcherPage> names = new ArrayList<NameResearcherPage>();

        Map<String, Set<Integer>> mapInvalids = new HashMap<String, Set<Integer>>();
        for (ResearcherPage researcher : rps)
        {
            Set<Integer> invalidIds = new HashSet<Integer>();

            List<RelationPreference> rejected = new ArrayList<RelationPreference>();
            for (RelationPreferenceConfiguration configuration : relationPreferenceService
                    .getConfigurationService().getList())
            {
                if (configuration.getRelationConfiguration().getRelationClass()
                        .equals(Item.class))
                {
                    rejected = relationPreferenceService
                            .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                    researcher.getUuid(), configuration
                                            .getRelationConfiguration()
                                            .getRelationName(),
                                    RelationPreference.UNLINKED);
                }
            }

            for (RelationPreference relationPreference : rejected)
            {
                invalidIds.add(relationPreference.getItemID());
            }
            
            mapInvalids.put(researcher.getCrisID(), invalidIds);
        }
        log.debug("...DONE building names list size " + names.size());
        log.debug("Create DSpace context and use browse indexing");
        Context context = null;
        try
        {
            context = new Context();
            context.setIgnoreAuthorization(true);

            IRetrievePotentialMatchPlugin plugin = new DSpace()
                    .getSingletonService(IRetrievePotentialMatchPlugin.class);
            
            Map<NameResearcherPage, Item[]> result = null;
            
            if(COMMAND_WORK.equals(command)) {
                //do work command
                result = plugin
                        .retrieveGroupByName(context, mapInvalids, rps, false);
                
                List<MetadataField> fieldsWithAuthoritySupport = metadataFieldWithAuthorityRP(context);
                for (NameResearcherPage tempName : result.keySet())
                {
                    if(result.get(tempName).length>0) {
                        bindItemsToRP(relationPreferenceService, context,
                                fieldsWithAuthoritySupport, tempName,
                                result.get(tempName));
                    }
                }
            }
            else {
                if(COMMAND_LISTEXCLUDEAUTHORITY.equals(command)) {
                    result = plugin
                            .retrieveGroupByNameExceptAuthority(context, mapInvalids, rps, true, true);                    
                }
                else if(COMMAND_LIST.equals(command)) {
                    result = plugin
                        .retrieveGroupByName(context, mapInvalids, rps, true);
                }
            }
            
            return result;

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

}
