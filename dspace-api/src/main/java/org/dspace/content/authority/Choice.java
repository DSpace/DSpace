/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.HashMap;
import java.util.Map;

/**
 * Record class to hold the data describing one option, or choice, for an
 * authority-controlled metadata value.
 *
 * @author Larry Stone
 * @see Choices
 */
public class Choice {
    /**
     * Authority key for this value
     */
    public String authority = null;

    /**
     * Label to display for this value (e.g. to present in UI menu)
     */
    public String label = null;

    /**
     * The canonical text value to insert into MetadataValue's text field
     */
    public String value = null;

    /**
     * A boolean representing if choice entry value can selected (usually true).
     * Hierarchical authority can flag some choice as not selectable to force the
     * use to choice a more detailed terms in the tree, such a leaf or a deeper
     * branch
     */
    public boolean selectable = true;

    /**
     * AuthorityName used as prefix for identifier, when is populated
     */
    public String authorityName = null;

    public Map<String, String> extras = new HashMap<String, String>();

    public String source = "local";

    public Choice() {
    }

    /**
     * Minimal constructor for this data object. It assumes an empty map of extras
     * information and a selected choice
     * 
     * @param authority the authority key
     * @param value     the text value to store in the metadata
     * @param label     the value to display to the user
     */
    public Choice(String authority, String value, String label) {
        this.authority = authority;
        this.value = value;
        this.label = label;
    }

    /**
     * Constructor to quickly setup the data object for basic authorities. The choice is assumed to be selectable.
     * 
     * @param authority the authority key
     * @param value     the text value to store in the metadata
     * @param label     the value to display to the user
     * @param extras    a key value map of extra information related to this choice
     */
    public Choice(String authority, String label, String value, Map<String, String> extras) {
        this.authority = authority;
        this.label = label;
        this.value = value;
        this.extras = extras;
    }

    /**
     * Constructor to quickly setup the data object for basic authorities. The choice is assumed to be selectable.
     *
     * @param authority the authority key
     * @param value     the text value to store in the metadata
     * @param label     the value to display to the user
     * @param extras    a key value map of extra information related to this choice
     * @param source    authority SOURCE reference
     */
    public Choice(String authority, String label, String value, Map<String, String> extras, String source) {
        this.authority = authority;
        this.label = label;
        this.value = value;
        this.extras = extras;
        this.source = source;
    }

    /**
     * Constructor for common need of Hierarchical authorities that want to
     * explicitely set the selectable flag
     * 
     * @param authority  the authority key
     * @param value      the text value to store in the metadata
     * @param label      the value to display to the user
     * @param selectable true if the choice can be selected, false if the a more
     *                   accurate choice should be preferred
     */
    public Choice(String authority, String label, String value, boolean selectable) {
        this.authority = authority;
        this.label = label;
        this.value = value;
        this.selectable = selectable;
    }

    /**
     * Constructor for common need of Hierarchical authorities that want to
     * explicitely set the selectable flag
     *
     * @param authorityName the authority name that can be used to fully-qualify the authority
     * @param authority     the authority key
     * @param value         the text value to store in the metadata
     * @param label         the value to display to the user
     * @param selectable    true if the choice can be selected, false if the a more
     *                      accurate choice should be preferred
     */
    public Choice(String authorityName, String authority, String label, String value, boolean selectable) {
        this.authorityName = authorityName;
        this.authority = authority;
        this.label = label;
        this.value = value;
        this.selectable = selectable;
    }
}
