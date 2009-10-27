/*
 * DCPersonName.java
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
package org.dspace.content;

/**
 * DSpace person name utility class
 * <P>
 * Person names in the Dublin Core value table in the DSpace database are stored
 * in the following simple format:
 * <P>
 * <code>Lastname, First name(s)</code>
 * <P>
 * <em>FIXME:  No policy for dealing with "van"/"van der" and "Jr."</em>
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class DCPersonName
{
    /** The person's last name */
    private String lastName;

    /** The person's first name(s) */
    private String firstNames;

    /** Construct a blank name */
    public DCPersonName()
    {
        lastName = null;
        firstNames = null;
    }

    /**
     * Construct a name from a raw DC value
     * 
     * @param rawValue
     *            the value entry from the database
     */
    public DCPersonName(String rawValue)
    {
        // Null by default (representing noone)
        lastName = null;
        firstNames = null;

        // Check we've actually been passed a name
        if ((rawValue != null) && !rawValue.equals(""))
        {
            // Extract the last name and first name components
            int commaIndex = rawValue.indexOf(',');

            // Just in case there's no comma, assume whole thing is
            // last name
            if (commaIndex == -1)
            {
                commaIndex = rawValue.length();
            }

            lastName = rawValue.substring(0, commaIndex);

            // Just in case the first name is blank
            if (rawValue.length() > (commaIndex + 2))
            {
                firstNames = rawValue.substring(commaIndex + 2);
            }
            else
            {
                // Since we have a name, we don't want to
                // leave the last name as null
                firstNames = "";
            }
        }
    }

    /**
     * Construct a name from a last name and first name
     * 
     * @param lastNameIn
     *            the last name
     * @param firstNamesIn
     *            the first names
     */
    public DCPersonName(String lastNameIn, String firstNamesIn)
    {
        lastName = lastNameIn;
        firstNames = firstNamesIn;
    }

    /**
     * Return a string for writing the name to the database
     * 
     * @return the name, suitable for putting in the database
     */
    public String toString()
    {
        StringBuffer out = new StringBuffer();

        if (lastName != null)
        {
            out.append(lastName);

            if ((firstNames != null) && !firstNames.equals(""))
            {
                out.append(", ").append(firstNames);
            }
        }

        return (out.toString());
    }

    /**
     * Get the first name(s). Guaranteed non-null.
     * 
     * @return the first name(s), or an empty string if none
     */
    public String getFirstNames()
    {
        return ((firstNames == null) ? "" : firstNames);
    }

    /**
     * Get the last name. Guaranteed non-null.
     * 
     * @return the last name, or an empty string if none
     */
    public String getLastName()
    {
        return ((lastName == null) ? "" : lastName);
    }
}
