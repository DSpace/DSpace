/*
 * DCLanguage.java
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

import java.util.Locale;

/**
 * Utility class for dealing with languages
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class DCLanguage
{
    /** The country code */
    private String country;

    /** The language code. Special values: "" and "other". */
    private String language;

    /**
     * Construct a language object from a database entry
     * 
     * @param l
     *            the language text from the database
     */
    public DCLanguage(String l)
    {
        setLanguage(l);
    }

    /**
     * Write the language out to the database
     * 
     * @return the language in a form for writing to the DCValue table
     */
    public String toString()
    {
        if (language.equals(""))
        {
            return "";
        }
        else if (country.equals(""))
        {
            return language;
        }
        else
        {
            return country + "_" + language;
        }
    }

    /**
     * Set the language and country
     * 
     * @param l
     *            The language and country code, e.g. "en_US" or "fr"
     */
    public void setLanguage(String l)
    {
        if (l.equals("other"))
        {
            language = "other";
            country = "";
        }
        else if (l.length() == 2)
        {
            language = l;
            country = "";
        }
        else if (l.length() == 5)
        {
            language = l.substring(0, 2);
            country = l.substring(3);
        }
        else
        {
            language = "";
            country = "";
        }
    }

    /**
     * Get the displayable name for this language
     * 
     * @return the displayable name
     */
    public String getDisplayName()
    {
        Locale locale;

        if (language.equals("other"))
        {
            return "(Other)";
        }
        else if (language.equals(""))
        {
            return "N/A";
        }
        else
        {
            locale = new Locale(language, country);

            return locale.getDisplayName();
        }
    }
}
