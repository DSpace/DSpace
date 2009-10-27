/*
 * PackageParameters.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

package org.dspace.content.packager;

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletRequest;

/**
 * Parameter list for SIP and DIP packagers. It's really just
 * a Java Properties object extended so each parameter can have
 * multiple values.  This was necessary so it can represent Servlet
 * parameters, which have multiple values.  It is also helpful to
 * indicate e.g. metadata choices for package formats like METS that
 * allow many different metadata segments.
 *
 * @author Larry Stone
 * @version $Revision$
 */

public class PackageParameters extends Properties
{
    // Use non-printing FS (file separator) as arg-sep token, like Perl $;
    private static final String SEPARATOR = "\034";

    // Regular expression to match the separator token:
    private static final String SEPARATOR_REGEX = "\\034";

    public PackageParameters()
    {
        super();
    }

    public PackageParameters(Properties defaults)
    {
        super(defaults);
    }

    /**
     * Creates new parameters object with the parameter values from
     * a servlet request object.
     *
     * @param request - the request from which to take the values
     * @return new parameters object.
     */
    public static PackageParameters create(ServletRequest request)
    {
        PackageParameters result = new PackageParameters();

        Enumeration pe = request.getParameterNames();
        while (pe.hasMoreElements())
        {
            String name = (String)pe.nextElement();
            String v[] = request.getParameterValues(name);
            if (v.length == 0)
                result.setProperty(name, "");
            else if (v.length == 1)
                result.setProperty(name, v[0]);
            else
            {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < v.length; ++i)
                {
                    if (i > 0)
                        sb.append(SEPARATOR);
                    sb.append(v[i]);
                }
                result.setProperty(name, sb.toString());
            }
        }
        return result;
    }


    /**
     * Adds a value to a property; if property already has value(s),
     * this is tacked onto the end, otherwise it acts like setProperty().
     *
     * @param key - the key to be placed into this property list.
     * @param value - the new value to add, corresponding to this key.
     * @return the previous value of the specified key in this property list, or
     *    null if it did not have one.
     */
    public Object addProperty(String key, String value)
    {
        String oldVal = getProperty(key);
        if (oldVal == null)
            setProperty(key, value);
        else
            setProperty(key, oldVal + SEPARATOR + value);
        return oldVal;
    }

    /**
     * Returns multiple property values in an array.
     *
     * @param key - the key to look for in this property list.
     * @return all values in an array, or null if this property is unset.
     */
    public String[] getProperties(String key)
    {
        String val = getProperty(key);
        if (val == null)
            return null;
        else
            return val.split(SEPARATOR_REGEX);
    }

    /**
     * Returns boolean form of property with selectable default
     * @param key the key to look for in this property list.
     * @param default default to return if there is no such property
     * @return the boolean derived from the value of property, or default
     *   if it was not specified.
     */
    public boolean getBooleanProperty(String key, boolean defaultAnswer)
    {
        String stringValue = getProperty(key);

        if (stringValue == null)
            return defaultAnswer;
        else
            return stringValue.equalsIgnoreCase("true") ||
                   stringValue.equalsIgnoreCase("on") ||
                   stringValue.equalsIgnoreCase("yes");
    }
}
