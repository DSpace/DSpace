/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Index values for each configured solr suggestion field to a _suggest solr field
 *
 * @author Kim Shepherd
 */
public class SolrServiceIndexSuggestFieldPlugin implements SolrServiceIndexPlugin {
    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger();

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected ConfigurationService configurationService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected MetadataExposureService metadataExposureService;

    @Override
    public void additionalIndex(Context context, IndexableObject idxObj, SolrInputDocument document) {

        if (!(idxObj instanceof IndexableItem)) {
            return;
        }
        if (!(((IndexableItem) idxObj).getIndexedObject() instanceof Item item)) {
            return;
        }

        if (!item.isDiscoverable() || item.isWithdrawn() || !item.isArchived()) {
            log.debug("Skipping suggestion term index from non-discoverable / non-archived item {}",
                    item.getID());
            return;
        }

        boolean contextAuthWasIgnored = context.ignoreAuthorization();
        try {
            // Most index and plugins want to ignore authorisation, so they can simply build the docs
            // and let Solr / Discovery endpoints / Auth service decide who can see them. In this case
            // we are adding a field which will be read directly by the Solr Suggest Handler so
            // we need to be more restrictive here. Restore authorisation so context is acting as
            // an anonymous user (public READ check, etc)
            if (contextAuthWasIgnored) {
                context.restoreAuthSystemState();
            }
            // Don't index values from items without public READ access
            if (!authorizeService.authorizeActionBoolean(context, item, Constants.READ)) {
                log.debug("Skipping suggestion term index from non-public item {}",
                        item.getID());
                return;
            }
            log.debug("Looking for suggestion fields in item {}.", item.getID());
            // Index all metadata fields configured as suggestion fields
            String[] suggestionFields = configurationService.getArrayProperty("discovery.suggest.field");
            for (String suggestionField : suggestionFields) {
                MetadataFieldName field = new MetadataFieldName(suggestionField);
                // Don't index metadata fields configured with metadata.hide.* in dspace.cfg
                if (metadataExposureService.isHidden(context,
                            field.schema, field.element, field.qualifier)) {
                    log.debug("Skipping suggestion term index of hidden field {}", field);
                    continue;
                }
                log.debug("Checking suggestion field {}.", suggestionField);
                List<MetadataValue> suggestionValues =
                    itemService.getMetadataByMetadataString(item, suggestionField);
                List<String> sv = new ArrayList<String>();
                for (MetadataValue v : suggestionValues) {
                    log.debug("Add value {} for suggestion field {}.", v.getValue(), suggestionField);
                    sv.add(v.getValue());
                }
                String docField = suggestionField + "_suggest";
                document.addField(docField, sv);
            }
        } catch (Exception e) {
            log.error("Error while indexing suggestion fields," +
                    "Item: (id " + item.getID() + " name " + item.getName() + ")");
        } finally {
            // Put context authZ state back the way we found it
            if (contextAuthWasIgnored) {
                context.turnOffAuthorisationSystem();
            }
        }
    }
}

