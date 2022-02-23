/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.processor;

import static org.dspace.app.ldn.LDNMetadataFields.ELEMENT;
import static org.dspace.app.ldn.LDNMetadataFields.SCHEMA;
import static org.dspace.content.Item.ANY;

public abstract class LDNMetadataChange {

    private String schema;

    private String element;

    private String language;

    private String conditionTemplate;

    public LDNMetadataChange() {
        schema = SCHEMA;
        element = ELEMENT;
        language = ANY;
        conditionTemplate = "true";
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getConditionTemplate() {
        return conditionTemplate;
    }

    public void setConditionTemplate(String conditionTemplate) {
        this.conditionTemplate = conditionTemplate;
    }

}
