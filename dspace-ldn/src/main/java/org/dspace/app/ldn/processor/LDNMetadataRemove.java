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

public class LDNMetadataRemove extends LDNMetadataChange {

    private List<String> qualifiers = new ArrayList<>();

    private List<String> valueTemplates = new ArrayList<>();

    public List<String> getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(List<String> qualifiers) {
        this.qualifiers = qualifiers;
    }

    public List<String> getValueTemplates() {
        return valueTemplates;
    }

    public void setValueTemplates(List<String> valueTemplates) {
        this.valueTemplates = valueTemplates;
    }

}
