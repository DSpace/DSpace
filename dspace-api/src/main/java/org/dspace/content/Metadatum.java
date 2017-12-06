/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.content.authority.Choices;

/**
 * Simple data structure-like class representing a flat metadata value. It has a
 * schema, element, qualifier, value, language and authority.
 *
 * @author Robert Tansley
 * @author Martin Hald
 * @version $Revision$
 */
public class Metadatum
{
    /** The element name. */
    public String element;

    /** The name's qualifier, or <code>null</code> if unqualified. */
    public String qualifier;

    /** The value of the field. */
    public String value;

    /** The language of the field, may be <code>null</code>. */
    public String language;

    /** The schema name of the metadata element. */
    public String schema;

    /** Authority control key. */
    public String authority = null;

    /** Authority control confidence. */
    public int confidence = Choices.CF_UNSET;



    public Metadatum copy() {
        Metadatum copy = new Metadatum();
        copy.value = this.value;
        copy.authority = this.authority;
        copy.confidence = this.confidence;
        copy.element = this.element;
        copy.language = this.language;
        copy.qualifier = this.qualifier;
        copy.schema = this.schema;
        return copy;
    }
    /**
     * Get the name of the field in dot notation:  schema.element.qualifier,
     * as in {@code dc.date.issued}.
     *
     * @return stringified name of this field.
     */
    public String getField() {
        return schema + "." + element + (qualifier==null?"":("." + qualifier));
    }

    public boolean hasSameFieldAs(Metadatum dcValue) {
        if (dcValue == this) {
            return true;
        }
        if (dcValue.element != null ? !dcValue.element.equals(this.element) : this.element != null) {
            return false;
        }
        if (dcValue.qualifier != null ? !dcValue.qualifier.equals(this.qualifier) : this.qualifier != null) {
            return false;
        }
        if (dcValue.schema != null ? !dcValue.schema.equals(this.schema) : this.schema != null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Metadatum dcValue = (Metadatum) o;

        if (confidence != dcValue.confidence) {
            return false;
        }
        if (authority != null ? !authority.equals(dcValue.authority) : dcValue.authority != null) {
            return false;
        }
        if (element != null ? !element.equals(dcValue.element) : dcValue.element != null) {
            return false;
        }
        if (language != null ? !language.equals(dcValue.language) : dcValue.language != null) {
            return false;
        }
        if (qualifier != null ? !qualifier.equals(dcValue.qualifier) : dcValue.qualifier != null) {
            return false;
        }
        if (schema != null ? !schema.equals(dcValue.schema) : dcValue.schema != null) {
            return false;
        }
        if (value != null ? !value.equals(dcValue.value) : dcValue.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = element != null ? element.hashCode() : 0;
        result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (schema != null ? schema.hashCode() : 0);
        result = 31 * result + (authority != null ? authority.hashCode() : 0);
        result = 31 * result + confidence;
        return result;
    }
}
