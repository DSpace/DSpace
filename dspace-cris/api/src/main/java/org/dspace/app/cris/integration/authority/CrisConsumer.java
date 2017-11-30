/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.integration.CRISAuthority;
import org.dspace.app.cris.integration.DOAuthority;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authority.AuthorityValueGenerator;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.utils.HashUtil;

/**
 */
public class CrisConsumer implements Consumer
{

    public static final String SOURCE_INTERNAL = "INTERNAL-SUBMISSION";

    private static final Logger log = Logger.getLogger(CrisConsumer.class);

    private SearchService searcher = new DSpace().getServiceManager()
            .getServiceByName(SearchService.class.getName(),
                    SearchService.class);

    private ApplicationService applicationService = new DSpace()
            .getServiceManager()
            .getServiceByName("applicationService", ApplicationService.class);

    private MetricsPersistenceService metricService = new DSpace()
            .getServiceManager().getServiceByName(MetricsPersistenceService.class.getName(),
                    MetricsPersistenceService.class);

    private transient Set<String> processedHandles = new HashSet<String>();

    public void initialize() throws Exception
    {
    }

    public void consume(Context ctx, Event event) throws Exception
    {
        DSpaceObject dso = event.getSubject(ctx);
        if (dso instanceof Item)
        {
            Item item = (Item) dso;
            if (item == null || !item.isArchived())
                return;
            if (processedHandles.contains(item.getHandle()))
            {
                return;
            }
            else
            {
                processedHandles.add(item.getHandle());
            }

            ctx.turnOffAuthorisationSystem();
            Set<String> listAuthoritiesManager = ChoiceAuthorityManager
                    .getManager().getAuthorities();

            Map<String, List<Metadatum>> toBuild = new HashMap<String, List<Metadatum>>();
            Map<String, List<Metadatum>> toUpdate = new HashMap<String, List<Metadatum>>();
            Map<String, String> toBuildType = new HashMap<String, String>();
            Map<String, CRISAuthority> toBuildChoice = new HashMap<String, CRISAuthority>();
            Map<String, String> toBuildMetadata = new HashMap<String, String>();

            for (String crisAuthority : listAuthoritiesManager)
            {
                List<String> listMetadata = ChoiceAuthorityManager.getManager()
                        .getAuthorityMetadataForAuthority(crisAuthority);

                for (String metadata : listMetadata)
                {
                    Metadatum[] Metadatums = item
                            .getMetadataByMetadataString(metadata);
                    ChoiceAuthority choiceAuthority = ChoiceAuthorityManager
                            .getManager().getChoiceAuthority(metadata);
                    if (CRISAuthority.class
                            .isAssignableFrom(choiceAuthority.getClass()))
                    {
                        int idx = 0;
                        for (Metadatum dcval : Metadatums)
                        {
                            dcval.setPlace(idx);
                            if(StringUtils.equals(dcval.value, MetadataValue.PARENT_PLACEHOLDER_VALUE)){
                            	idx++;
                            	continue;
                            }
                            String authority = dcval.authority;
                            if (StringUtils.isNotBlank(authority))
                            {
                                String type = null, info = null;
                                if (authority.startsWith(
                                        AuthorityValueGenerator.GENERATE))
                                {

                                    String[] split = StringUtils.split(
                                            authority,
                                            AuthorityValueGenerator.SPLIT);
                                    if (split.length > 0)
                                    {
                                        type = split[1];
                                        if (split.length > 1)
                                        {
                                            info = split[2];
                                        }
                                    }
                                }
                                else {
                                    type = SOURCE_INTERNAL;
                                    info = dcval.authority;
                                }
                                toBuildType.put(info, type);

                                List<Metadatum> list = new ArrayList<Metadatum>();
                                if (toBuild.containsKey(info))
                                {
                                    list = toBuild.get(info);
                                    list.add(dcval);
                                }
                                else
                                {
                                    list.add(dcval);
                                }
                                toBuild.put(info, list);
                                toBuildChoice.put(info,
                                        (CRISAuthority) choiceAuthority);
                                toBuildMetadata.put(info, metadata);
                            }
                            else
                            {
                                boolean activateImportInSubmission = ConfigurationManager
                                        .getBooleanProperty("cris",
                                                "import.submission." + metadata,
                                                "import.submission");
                                if (activateImportInSubmission)
                                {
                                    String valueHashed = "";

                                    boolean buildFromUUID = ConfigurationManager
                                            .getBooleanProperty("cris",
                                                    "import.submission.strategy.uuid."
                                                            + metadata,
                                                    false);
                                    if (buildFromUUID)
                                    {
                                        valueHashed = UUID.randomUUID()
                                                .toString();
                                    }
                                    else
                                    {
                                        valueHashed = HashUtil
                                                .hashMD5(dcval.value);
                                    }
                                        
                                    List<Metadatum> list = new ArrayList<Metadatum>();
                                    if (toBuild.containsKey(valueHashed))
                                    {
                                        list = toBuild.get(valueHashed);
                                        list.add(dcval);
                                    }
                                    else
                                    {
                                        list.add(dcval);
                                    }
                                    toBuild.put(valueHashed, list);
                                    toBuildType.put(valueHashed,
                                            SOURCE_INTERNAL);
                                    toBuildChoice.put(valueHashed,
                                            (CRISAuthority) choiceAuthority);
                                    toBuildMetadata.put(valueHashed, metadata);
                                }
                            }
                            idx++;
                        }
                    }
                }
            }

            Map<String, String> toBuildAuthority = new HashMap<String, String>();
            Map<String, ACrisObject> createdObjects = new HashMap<String, ACrisObject>();
            Map<String, ACrisObject> referencedObjects = new HashMap<String, ACrisObject>();

            for (String authorityKey : toBuild.keySet())
            {

                String rpKey = null;

                CRISAuthority choiceAuthorityObject = toBuildChoice
                        .get(authorityKey);
                String typeAuthority = toBuildType.get(authorityKey);

                Class<ACrisObject> crisTargetClass = choiceAuthorityObject
                        .getCRISTargetClass();
                
                ACrisObject rp = applicationService.getEntityBySourceId(
                        typeAuthority, authorityKey, crisTargetClass);
                if(rp==null) {
                    rp = applicationService.getEntityByCrisId(authorityKey, crisTargetClass);
                }

                if (rp != null)
                {
                    rpKey = rp.getCrisID();
                    referencedObjects.put(rpKey, rp);
                }
                else
                {
                    // build a simple RP
                    rp = choiceAuthorityObject.getNewCrisObject();
                    SolrQuery query = new SolrQuery();

                    if (choiceAuthorityObject.getCRISTargetTypeID() == -1)
                    {
                        query.setQuery("search.resourcetype:[1001 TO 9999]");
                    }
                    else
                    {
                        query.setQuery("search.resourcetype:"
                                + choiceAuthorityObject.getCRISTargetTypeID());
                    }

                    if (StringUtils.isNotBlank(authorityKey))
                    {
                        if ((typeAuthority.equalsIgnoreCase(SOURCE_INTERNAL)))
                        {
                            query.addFilterQuery(
                                    "cris-sourceref:" + typeAuthority);
                            query.addFilterQuery(
                                    "cris-sourceid:" + authorityKey);
                        }
                        else
                        {
                            String filterQuery = "cris" + rp.getPublicPath()
                                    + "." + typeAuthority.toLowerCase() + ":\""
                                    + authorityKey + "\"";
                            query.addFilterQuery(filterQuery);
                        }
                    }

                    QueryResponse qResp = searcher.search(query);
                    SolrDocumentList docList = qResp.getResults();
                    if (docList.size() > 0)
                    {
                        SolrDocument doc = docList.get(0);
                        rpKey = (String) doc.getFirstValue("cris"
                                + rp.getPublicPath() + ".this_authority");
                    }

                    if (rpKey == null)
                    {

                        rp.setSourceID(authorityKey);
                        rp.setSourceRef(typeAuthority);

                        List<Metadatum> MetadatumAuthority = toBuild
                                .get(authorityKey);
                        String prefix = "";
                        if (choiceAuthorityObject instanceof DOAuthority)
                        {
                            prefix = ConfigurationManager.getProperty("cris",
                                    "DOAuthority."
                                            + toBuildMetadata.get(authorityKey)
                                            + ".new-instances");
                            if (StringUtils.isNotBlank(prefix))
                            {
                                DynamicObjectType dType = applicationService
                                        .findTypoByShortName(
                                                DynamicObjectType.class,
                                                prefix);
                                if (dType != null)
                                {
                                    ((ResearchObject) rp).setTypo(dType);
                                }
                                else
                                {
                                    continue;
                                }
                            }
                            else
                            {
                                continue;
                            }
                        }

                        ResearcherPageUtils.buildTextValue(rp,
                                MetadatumAuthority.get(0).value,
                                prefix + rp.getMetadataFieldTitle());

                        boolean activateNewObject = ConfigurationManager
                                .getBooleanProperty("cris",
                                        "import.submission.enabled.entity."
                                                + toBuildMetadata
                                                        .get(authorityKey),
                                        "import.submission.enabled.entity");
                        if (activateNewObject)
                        {
                            rp.setStatus(true);
                        }

                        try
                        {
                            applicationService.saveOrUpdate(crisTargetClass,
                                    rp);
                            log.info("Build new CRIS Object [" + crisTargetClass
                                    + "] sourceId/sourceRef:" + authorityKey
                                    + " / " + typeAuthority);
                        }
                        catch (Exception ex)
                        {
                            log.error(ex.getMessage(), ex);
                        }
                        rpKey = rp.getCrisID();
                        createdObjects.put(authorityKey, rp);
                    }
                    else
                    {
                        referencedObjects.put(authorityKey, rp);
                    }
                }

                if (StringUtils.isNotBlank(rpKey))
                {
                    toBuildAuthority.put(authorityKey, rpKey);
                }
            }

            for (String orcid : toBuildAuthority.keySet())
            {
                for (Metadatum Metadatum : toBuild.get(orcid))
                {
                    Metadatum newValue = Metadatum.copy();
                    newValue.authority = toBuildAuthority.get(orcid);
                    newValue.confidence = Choices.CF_ACCEPTED;
                    newValue.setPlace(Metadatum.getPlace());
                    item.replaceMetadataValue(Metadatum, newValue);
                }
            }
            item.update();
            ctx.getDBConnection().commit();

            fillerAuthority(ctx, item, toBuild, toBuildType, createdObjects,
                    referencedObjects);

            fillerMetrics(ctx, item, toBuildMetadata, createdObjects, referencedObjects);

            ctx.restoreAuthSystemState();
        }
    }

    private void fillerAuthority(Context ctx, Item item,
            Map<String, List<Metadatum>> toBuild,
            Map<String, String> toBuildType,
            Map<String, ACrisObject> createdObjects,
            Map<String, ACrisObject> referencedObjects)
    {
        Map<String, ACrisObject> extraUpdateObjects = new HashMap<String, ACrisObject>();
        AuthoritiesFillerConfig fillerConfig = new DSpace()
                .getSingletonService(AuthoritiesFillerConfig.class);

        for (String authorityKey : toBuild.keySet())
        {
            // enhrich the new cris objects as much as possible
            ACrisObject rp = createdObjects.get(authorityKey);
            boolean isUpdate = false;
            if (rp == null)
            {
                rp = referencedObjects.get(authorityKey);
                isUpdate = true;
            }
            String typeAuthority = toBuildType.get(authorityKey);
            ImportAuthorityFiller filler = fillerConfig
                    .getFiller(typeAuthority);
            log.debug("crisconsumer -> filler -> " + typeAuthority);
            log.debug("crisconsumer -> filler -> " + rp != null + " " + rp);
            log.debug("crisconsumer -> filler -> " + authorityKey);
            if (filler != null && (!isUpdate || filler.allowsUpdate(ctx, item,
                    toBuild.get(authorityKey), authorityKey, rp)))
            {
                filler.fillRecord(ctx, item, toBuild.get(authorityKey),
                        authorityKey, rp);
                extraUpdateObjects.put(authorityKey, rp);
            }
            // update is done at the end to assure that all the new entities are
            // full populated before the final indexing
            // applicationService.saveOrUpdate(rp.getCRISTargetClass(), rp);
        }

        for (ACrisObject rp : extraUpdateObjects.values())
        {
            // persist the information, this will produce reindexing of the new
            // entity with the full information
            if (rp == null)
            {
                continue;
            }
            applicationService.saveOrUpdate(rp.getCRISTargetClass(), rp);
        }
    }

    private void fillerMetrics(Context ctx, Item item,
            Map<String, String> toBuildMetadata,
            Map<String, ACrisObject> createdObjects,
            Map<String, ACrisObject> referencedObjects)
    {
        MetricImportFiller fillerConfig = new DSpace()
                .getSingletonService(MetricImportFiller.class);

        if (fillerConfig != null)
        {
            Map<String, TargetMetricFillerPlugin> plugins = fillerConfig
                    .getPlugins();
            if (plugins != null)
            {
                for (String metadataMetric : plugins.keySet())
                {

                    Metadatum[] metadata = item
                            .getMetadataValueInDCFormat(metadataMetric);
                    if (metadata != null && metadata.length > 0)
                    {
                        TargetMetricFillerPlugin plugin = plugins
                                .get(metadataMetric);
                        plugin.buildMetric(ctx, item, metadata[0],
                                toBuildMetadata, createdObjects,
                                referencedObjects, applicationService,
                                metricService);
                    }
                }
            }
        }

    }

    public void end(Context ctx) throws Exception
    {
        // nothing to do
        processedHandles.clear();
    }

    public void finish(Context ctx) throws Exception
    {
        // nothing to do
    }
}