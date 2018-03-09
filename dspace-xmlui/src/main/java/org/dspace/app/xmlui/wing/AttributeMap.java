/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing;

import java.util.HashMap;

/**
 * A simplified Wing version of an AttributeMap. If a namespace is set it is
 * applied to all attributes in the Map. This is typically not what one would
 * expect but it makes the syntax easier for Wing Elements. Either all the
 * attributes come from one namespace or the other, but never two at the same
 * time.
 * 
 * @author Scott Phillips
 */

public class AttributeMap extends HashMap<String, String>
{
    /** Just so there are no compile warnings */
    private static final long serialVersionUID = 1;

    /** The namespace of ALL the attributes contained within this map */
    private Namespace namespace;

    /**
     * Set the namespace for all attributes contained within this map.
     * 
     * @param namespace
     *            The new namespace.
     */
    public void setNamespace(Namespace namespace)
    {
        this.namespace = namespace;
    }

    /**
     * 
     * @return The namespace for all the attributes contained within this map.
     */
    public Namespace getNamespace()
    {
        return this.namespace;
    }

    /**
     * Another variation of the put method to convert integer values into
     * strings.
     * 
     * @param key
     *            The attribute's name.
     * @param value
     *            The value of the attribute.
     * @return previous value bound to the key, if any.
     */
    public String put(String key, int value)
    {
        return this.put(key, String.valueOf(value));
    }

    /**
     * Another variation of the put method to convert boolean values into
     * strings. The values "yes" or "no" will be used in replacement of the
     * boolean value.
     * 
     * @param key the attribute's name.
     * @param value the value of the attribute.
     * @return previous value bound to the key, if any.
     */
    public String put(String key, boolean value)
    {
        return this.put(key, (value ? "yes" : "no"));
    }
}
