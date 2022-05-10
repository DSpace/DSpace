/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

/**
 * Instuctions for removing metadata during notification processing.
 * 
 * @author William Welling
 * @author Stefano Maffei (4Science.com)
 * 
 */
public class LDNMetadataRemove extends LDNMetadataChange {

    private List<String> qualifiers = new ArrayList<>();

    // velocity templates with notification as it contexts
    private List<String> valueTemplates = new ArrayList<>();

    /**
     * @return List<String>
     */
    public List<String> getQualifiers() {
        return qualifiers;
    }

    /**
     * @param qualifiers
     */
    public void setQualifiers(List<String> qualifiers) {
        this.qualifiers = qualifiers;
    }

    /**
     * @return List<String>
     */
    public List<String> getValueTemplates() {
        return valueTemplates;
    }

    /**
     * @param valueTemplates
     */
    public void setValueTemplates(List<String> valueTemplates) {
        this.valueTemplates = valueTemplates;
    }


    @Override
    public void doAction(VelocityContext velocityContext, VelocityEngine velocityEngine,
        Context context, Item item) throws Exception {
        List<MetadataValue> metadataValuesToRemove = new ArrayList<>();
        for (String qualifier : getQualifiers()) {
            List<MetadataValue> itemMetadata = itemService.getMetadata(
                    item,
                    getSchema(),
                    getElement(),
                    qualifier,
                    Item.ANY);

            for (MetadataValue metadatum : itemMetadata) {
                boolean delete = true;
                for (String valueTemplate : getValueTemplates()) {
                    String value = renderTemplate(velocityContext, velocityEngine, valueTemplate);
                    if (!metadatum.getValue().contains(value)) {
                        delete = false;
                    }
                }
                if (delete) {
                    log.info("Removing {}.{}.{} {} {}",
                            getSchema(),
                            getElement(),
                            qualifier,
                            getLanguage(),
                            metadatum.getValue());

                    metadataValuesToRemove.add(metadatum);
                }
            }
        }

        if (!metadataValuesToRemove.isEmpty()) {
            itemService.removeMetadataValues(context, item, metadataValuesToRemove);
        }
    }

}
