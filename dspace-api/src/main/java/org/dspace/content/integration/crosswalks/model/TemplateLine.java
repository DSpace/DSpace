/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.model;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

/**
 * Models a template line.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class TemplateLine {

    private static final String VIRTUAL_FIELD = "virtual";

    private static final String GROUP_FIELD = "group";

    private static final String GROUP_START_FIELD = "start";

    private static final String GROUP_END_FIELD = "end";

    private final String beforeField;

    private final String afterField;

    private final String field;

    private final String[] fieldBits;

    /**
     * Constructor with beforeField only to map lines without a field.
     *
     * @param beforeField the section of the line before the field
     */
    public TemplateLine(String beforeField) {
        this(beforeField, null, null);
    }

    /**
     * Full constructor.
     *
     * @param beforeField the section of the line before the field
     * @param afterField the section of the line after the field
     * @param field the field
     */
    public TemplateLine(String beforeField, String afterField, String field) {
        super();
        this.beforeField = beforeField;
        this.afterField = afterField;
        this.field = field;
        this.fieldBits = field != null ? field.split("\\.") : null;
    }

    public String getBeforeField() {
        return beforeField;
    }

    public String getAfterField() {
        return afterField;
    }

    public String getField() {
        return field;
    }

    public String[] getFieldBits() {
        return fieldBits;
    }

    public boolean isVirtualField() {
        return isNotEmpty(fieldBits) && fieldBits.length > 1 && VIRTUAL_FIELD.equals(fieldBits[0]);
    }

    public boolean isMetadataGroupStartField() {
        return isMetadataGroupField() && GROUP_START_FIELD.equals(fieldBits[2]);
    }

    public boolean isMetadataGroupEndField() {
        return isMetadataGroupField() && GROUP_END_FIELD.equals(fieldBits[2]);
    }

    public boolean isMetadataGroupField() {
        return isNotEmpty(fieldBits) && fieldBits.length == 3 && GROUP_FIELD.equals(fieldBits[0]);
    }

    public String getVirtualFieldName() {
        return isVirtualField() ? fieldBits[1] : null;
    }

    public String getMetadataGroupFieldName() {
        return isMetadataGroupField() ? fieldBits[1].replaceAll("-", ".") : null;
    }

}
