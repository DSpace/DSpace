/*
 * AttributeMap.java
 *
 * Version: $Revision: 1.2 $
 *
 * Date: $Date: 2005/10/21 21:16:33 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
     * @return
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
     * @param key
     * @param value
     * @return
     */
    public String put(String key, boolean value)
    {
        return this.put(key, (value ? "yes" : "no"));
    }
}
