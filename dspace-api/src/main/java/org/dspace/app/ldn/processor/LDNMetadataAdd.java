/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.processor;

/**
 * Instuctions for adding metadata during notification processing.
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

}