/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.processor;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Instuctions for adding metadata during notification processing.
 * 
 * @author William Welling
 * @author Stefano Maffei (4Science.com)
 * 
 */
public class LDNMetadataAdd extends LDNMetadataChange {

    private String qualifier;

    // velocity template with notification as it contexts
    private String valueTemplate;

    /**
     * @return String
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * @param qualifier
     */
    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    /**
     * @return String
     */
    public String getValueTemplate() {
        return valueTemplate;
    }

    /**
     * @param valueTemplate
     */
    public void setValueTemplate(String valueTemplate) {
        this.valueTemplate = valueTemplate;
    }


    @Override
    public void doAction(VelocityContext velocityContext, VelocityEngine velocityEngine,
        Context context, Item item) throws Exception {
        String value = renderTemplate(velocityContext, velocityEngine, getValueTemplate());
        log.info(
                "Adding {}.{}.{} {} {}",
                getSchema(),
                getElement(),
                getQualifier(),
                getLanguage(),
                value);

        itemService.addMetadata(
                context,
                item,
                getSchema(),
                getElement(),
                getQualifier(),
                getLanguage(),
                value);

    }

}
