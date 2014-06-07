/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import it.cilea.osd.jdyna.value.TextValue;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.VisibilityConstants;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.pmc.model.PMCCitation;
import org.dspace.app.cris.pmc.services.PMCPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class ScriptAddPMCDataToRP
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ScriptAddPMCDataToRP.class);

    private static String itemsInPubmedTP = ConfigurationManager
            .getProperty(CrisConstants.CFG_MODULE, "pmcdata.itemsInPubmedTP");

    private static String itemsInPMCTP = ConfigurationManager
            .getProperty(CrisConstants.CFG_MODULE, "pmcdata.itemsInPMCTP");

    private static String citationsTP = ConfigurationManager
            .getProperty(CrisConstants.CFG_MODULE, "pmcdata.citationsTP");

    private static String itemsCitedTP = ConfigurationManager
            .getProperty(CrisConstants.CFG_MODULE, "pmcdata.itemsCitedTP");

    /**
     * Batch script to aggregate PMC data to RPs. See the technical
     * documentation for further details.
     * 
     * @throws SearchServiceException
     */
    public static void main(String[] args) throws ParseException, SQLException,
            SearchServiceException
    {
        log.info("#### START AddPMCDataToRP: -----" + new Date()
                + " ----- ####");

        DSpace dspace = new DSpace();
        ApplicationService applicationService = dspace.getServiceManager()
                .getServiceByName("applicationService",
                        ApplicationService.class);

        CrisSearchService searchService = dspace.getServiceManager()
                .getServiceByName(CrisSearchService.class.getName(),
                		CrisSearchService.class);
        PMCPersistenceService pmcService = dspace.getServiceManager()
                .getServiceByName(PMCPersistenceService.class.getName(),
                        PMCPersistenceService.class);

        List<ResearcherPage> rs = applicationService
                .getList(ResearcherPage.class);

        for (ResearcherPage rp : rs)
        {
            boolean updated = false;
            int itemsCited = 0;
            int citations = 0;
            SolrQuery query = new SolrQuery();
            query.setQuery("dc.identifier.pmid:[* TO *]");
            query.addFilterQuery("{!field f=author_authority}"
                    + ResearcherPageUtils.getPersistentIdentifier(rp),"NOT(withdrawn:true)");
            query.setFields("dc.identifier.pmid");
            query.setRows(Integer.MAX_VALUE);

            QueryResponse response = searchService.search(query);
            SolrDocumentList results = response.getResults();
            for (SolrDocument doc : results)
            {
                Integer pmid = null;
                try
                {
                    pmid = Integer.valueOf((String) doc
                            .getFirstValue("dc.identifier.pmid"));
                }
                catch (NumberFormatException e)
                {
                    log.warn("Found invalid pmid: "
                            + doc.getFieldValue("dc.identifier.pmid")
                            + " for rp: "
                            + ResearcherPageUtils.getPersistentIdentifier(rp));
                }
                if (pmid != null)
                {
                    PMCCitation pmccitation = pmcService.get(PMCCitation.class,
                            pmid);
                    if (pmccitation != null && pmccitation.getNumCitations() > 0)
                    {
                        itemsCited++;
                        citations += pmccitation.getNumCitations();
                    }
                }
            }

            updated = setValue(applicationService, rp, itemsCitedTP,
                    String.valueOf(itemsCited));
            // caution don't use the short-circuit OR operator (i.e || otherwise
            // only the first found pmcdata value will be recorded!) 
            updated = updated
                    | setValue(applicationService, rp, citationsTP,
                            String.valueOf(citations));
            updated = updated
                    | setValue(applicationService, rp, itemsInPubmedTP,
                            String.valueOf(results.getNumFound()));

            if (StringUtils.isNotEmpty(itemsInPMCTP))
            {
                query = new SolrQuery();
                query.setQuery("dc.identifier.pmcid:[* TO *]");
                query.addFilterQuery("{!field f=author_authority}"
                        + ResearcherPageUtils.getPersistentIdentifier(rp),"NOT(withdrawn:true)");
                query.setRows(0);

                response = searchService.search(query);
                results = response.getResults();
                // caution don't use the short-circuit OR operator (i.e || otherwise
                // only the first found pmcdata value will be recorded!)
                updated = updated
                        | setValue(applicationService, rp, itemsInPMCTP,
                                String.valueOf(results.getNumFound()));
            }

            if (updated)
            {
                applicationService.saveOrUpdate(ResearcherPage.class, rp);
            }
        }
        log.info("#### END AddPMCDataToRP: -----" + new Date() + " ----- ####");
    }

    private static boolean setValue(ApplicationService applicationService,
            ResearcherPage rp, String propDefName, String value)
    {
        boolean updated = false;
        List<RPProperty> currProps = rp.getDynamicField().getAnagrafica4view()
                .get(propDefName);
        if (currProps.size() == 0)
        {
            if (!value.equalsIgnoreCase("0"))
            {
                RPPropertiesDefinition propDef = applicationService
                        .findPropertiesDefinitionByShortName(
                                RPPropertiesDefinition.class, propDefName);
                RPProperty rpItemsCited = rp.getDynamicField().createProprieta(
                        propDef);
                TextValue valore = new TextValue();
                valore.setOggetto(value);
                rpItemsCited.setValue(valore);
                rpItemsCited.setVisibility(VisibilityConstants.PUBLIC);
                updated = true;
            }
        }
        else
        {
            RPProperty rpProperty = currProps.get(0);

            if (!value.equalsIgnoreCase("0"))
            {
                if (!value.equalsIgnoreCase((String) rpProperty.getObject()))
                {
                    rpProperty.getValue().setOggetto(value);
                    updated = true;
                }
            }
            else
            {
                rpProperty.getParent().removeProprieta(rpProperty);
                updated = true;
            }
        }
        return updated;
    }
}
