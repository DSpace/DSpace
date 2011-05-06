/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

/**
 * Record class to hold the data describing one option, or choice, for an
 * authority-controlled metadata value.
 *
 * @author Larry Stone
 * @see Choices
 */
public class Choice
{
    /** Authority key for this value */
    public String authority = null;

    /**  Label to display for this value (e.g. to present in UI menu) */
    public String label = null;

    /**  The canonical text value to insert into MetadataValue's text field */
    public String value = null;

    public Choice()
    {
    }

    public Choice(String authority, String value, String label)
    {
        this.authority = authority;
        this.value = value;
        this.label = label;
    }
}
