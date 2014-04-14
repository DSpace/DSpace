/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
    public final void setLanguage(String l)
    {
        if(l == null)
        {
            language = "";
            country = "";
        }
        else if("other".equals(l))
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
