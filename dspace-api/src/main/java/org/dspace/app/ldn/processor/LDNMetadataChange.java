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

/**
 * Base instructions for metadata change during notification processing.
 */
public abstract class LDNMetadataChange {

    private String schema;

    private String element;

    private String language;

    // velocity template with notification as its context
    private String conditionTemplate;

    /**
     * Default coar schema, notify element, any language, and true condition to
     * apply metadata change.
     */
    public LDNMetadataChange() {
        schema = SCHEMA;
        element = ELEMENT;
        language = ANY;
        conditionTemplate = "true";
    }

    /**
     * @return String
     */
    public String getSchema() {
        return schema;
    }

    /**
     * @param schema
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * @return String
     */
    public String getElement() {
        return element;
    }

    /**
     * @param element
     */
    public void setElement(String element) {
        this.element = element;
    }

    /**
     * @return String
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @return String
     */
    public String getConditionTemplate() {
        return conditionTemplate;
    }

    /**
     * @param conditionTemplate
     */
    public void setConditionTemplate(String conditionTemplate) {
        this.conditionTemplate = conditionTemplate;
    }

}