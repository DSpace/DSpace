/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoverySearchFilter;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

public class CRISReadPolicySolrIndexPlugin implements CrisServiceIndexPlugin
{
    Logger log = Logger.getLogger(CRISReadPolicySolrIndexPlugin.class);

    private CrisSearchService searchService;

    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject,
            SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        if (crisObject != null)
        {
            if (crisObject.getType() == CrisConstants.RP_TYPE_ID
                    || crisObject.getType() == CrisConstants.OU_TYPE_ID)
            {
                document.addField("read", "g0");
            }
            else
            {
                if (crisObject.getType() == CrisConstants.PROJECT_TYPE_ID
                        || (crisObject
                                .getType() > CrisConstants.CRIS_DYNAMIC_TYPE_ID_START
                                && crisObject
                                        .getType() < CrisConstants.CRIS_NDYNAMIC_TYPE_ID_START))
                {
                    String prefix = "";
                    if (crisObject
                            .getType() > CrisConstants.CRIS_DYNAMIC_TYPE_ID_START)
                    {
                        prefix = crisObject.getAuthorityPrefix();
                    }

                    String metadata = crisObject.getMetadata(prefix+"identifier");

                    if (StringUtils.isBlank(metadata))
                    {
                        metadata = crisObject.getName();
                    }
                    if (StringUtils.isNotBlank(metadata)
                            && StringUtils.isNumeric(metadata))
                    {

                        Context context = null;
                        Item dso = null;
                        try
                        {
                            context = new Context();
                            dso = Item.find(context,
                                    Integer.parseInt(metadata));
                            List<ResourcePolicy> policies = AuthorizeManager
                                    .getPoliciesActionFilter(context, dso,
                                            Constants.READ);
                            for (ResourcePolicy resourcePolicy : policies)
                            {
                                String fieldValue;
                                if (resourcePolicy.getGroupID() != -1)
                                {
                                    // We have a group add it to the value
                                    fieldValue = "g"
                                            + resourcePolicy.getGroupID();
                                }
                                else
                                {
                                    // We have an eperson add it to the value
                                    fieldValue = "e"
                                            + resourcePolicy.getEPersonID();

                                }

                                document.addField("read", fieldValue);
                            }
                        }
                        catch (SQLException e)
                        {
                            log.error(
                                    LogManager.getHeader(context,
                                            "Error while indexing resource policies",
                                            "DSpace object: (id " + dso.getID()
                                                    + " type " + dso.getType()
                                                    + ")"));
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
            }
        }
    }

    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACNO dso, SolrInputDocument sorlDoc, Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        // FIXME NOT SUPPORTED OPERATION
    }

    public void setSearchService(CrisSearchService searchService)
    {
        this.searchService = searchService;
    }

}
