/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.model;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

import org.dspace.content.integration.crosswalks.TabularCrosswalk;

/**
 * Models a line of the tabular template used in {@link TabularCrosswalk}.
 * Each line represents a <label, field> pair whose label represents the header
 * of the table and the field indicates the way to calculate the values for each item
 * (metadata, virtual field, nested metadata).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class TabularTemplateLine {

    private static final String SEPARATOR = "=";

    private static final String VIRTUAL_FIELD = "virtual";

    private static final String GROUP_FIELD = "group";

    private final String label;

    private final String field;

    private final String[] fieldBits;

    private TabularTemplateLine(String label, String field) {
        super();
        this.label = label;
        this.field = field;
        this.fieldBits = field != null ? field.split("\\.") : null;
    }

    /**
     * Build an instance of {@link TabularTemplateLine} starting from a template line.
     * The template line must have the format {label} = {field}.
     * @param line the template line
     * @return the instance of {@link TabularTemplateLine}
     */
    public static TabularTemplateLine fromLine(String line) {
        if (!line.contains(SEPARATOR)) {
            throw new IllegalStateException("The line '" + line + "' has no separator '" + SEPARATOR + "'");
        }

        String[] lineSections = line.split(SEPARATOR);
        if (lineSections.length != 2) {
            throw new IllegalStateException("The line '" + line + "' is not valid");
        }

        return new TabularTemplateLine(lineSections[0].trim(), lineSections[1].trim());
    }

    public String getLabel() {
        return label;
    }

    public String getField() {
        return field;
    }

    public boolean isVirtualField() {
        return isNotEmpty(fieldBits) && fieldBits.length > 1 && VIRTUAL_FIELD.equals(fieldBits[0]);
    }

    public boolean isMetadataGroupField() {
        return isNotEmpty(fieldBits) && fieldBits.length == 2 && GROUP_FIELD.equals(fieldBits[0]);
    }

    public String getVirtualFieldName() {
        return isVirtualField() ? fieldBits[1] : null;
    }

    public String getMetadataGroupFieldName() {
        return isMetadataGroupField() ? fieldBits[1].replaceAll("-", ".") : null;
    }

}
