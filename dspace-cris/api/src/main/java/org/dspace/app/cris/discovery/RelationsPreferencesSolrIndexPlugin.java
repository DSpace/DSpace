/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.configuration.DiscoverySearchFilter;

public class RelationsPreferencesSolrIndexPlugin implements
        CrisServiceIndexPlugin, SolrServiceIndexPlugin
{
    private static final Logger log = Logger
            .getLogger(RelationsPreferencesSolrIndexPlugin.class);

    private ApplicationService applicationService;

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> dso, SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        ACrisObject<P, TP, NP, NTP, ACNO, ATNO> item = dso;
        List<RelationPreference> preferences = applicationService
                .findRelationsPreferencesForUUID(item.getUuid());
        if (preferences != null)
        {
            for (RelationPreference rp : preferences)
            {
                String uuid = rp.getSourceUUID();
                String status = rp.getStatus();
                String fieldName = RelationPreference.PREFIX_RELATIONPREFERENCES
                        + rp.getRelationType() + "." + status.toLowerCase();
                document.addField(fieldName, uuid);
                if (rp.getPriority() > 0)
                {
                    for (int idx = 100; idx - rp.getPriority() > 0; idx--)
                    {
                        document.addField(fieldName, uuid);
                    }
                }
            }
        }

    }

    @Override
    public void additionalIndex(Context context, DSpaceObject dso,
            SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        if (!(dso instanceof Item))
            return;
        Item item = (Item) dso;
        int itemID = item.getID();
        List<RelationPreference> preferences = applicationService
                .findRelationsPreferencesForItemID(itemID);
        if (preferences != null)
        {
            for (RelationPreference rp : preferences)
            {
                String uuid = rp.getSourceUUID();
                String status = rp.getStatus();
                String fieldName = RelationPreference.PREFIX_RELATIONPREFERENCES
                        + rp.getRelationType() + "." + status.toLowerCase();
                document.addField(fieldName, uuid);
                if (rp.getPriority() > 0)
                {
                    for (int idx = 100; idx - rp.getPriority() > 0; idx--)
                    {
                        document.addField(fieldName, uuid);
                    }
                }
            }
        }
    }

	@Override
	public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
			ACNO dso, SolrInputDocument sorlDoc, Map<String, List<DiscoverySearchFilter>> searchFilters) {
		// FIXME NOT SUPPORTED OPERATION
	}
}
