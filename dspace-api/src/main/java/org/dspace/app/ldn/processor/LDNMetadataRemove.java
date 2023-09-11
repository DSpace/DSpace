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

/**
 * Instuctions for removing metadata during notification processing.
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

}