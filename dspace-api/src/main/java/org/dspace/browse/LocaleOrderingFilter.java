/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.util.Locale;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.text.filter.TextFilter;

import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

/**
 * Makes a sort string that is Locale dependent.
 * Uses the same Locale for all items, regardless of source language.
 * 
 * You can set the Locale to use by setting 'webui.browse.sort.locale'
 * in the dspace.cfg to an ISO code.
 * 
 * If you do not specify a Locale, then it defaults to Locale.ENGLISH.
 * 
 * IMPORTANT: The strings that this generates are NOT human readable.
 * Also, you will not be able to meaningfully apply any filters *after* this,
 * however, you can apply other filters before.
 * 
 * @author Graham Triggs
 */
public class LocaleOrderingFilter implements TextFilter
{
    private static Logger log = Logger.getLogger(LocaleOrderingFilter.class);

    /**
     * Uses a Locale dependent Collator to generate a sort string
     * @param str The string to parse
     * @return String the sort ordering text
     */
    @Override
    public String filter(String str)
    {
        RuleBasedCollator collator = getCollator();

        // Have we got a collator?
        if (collator != null)
        {
            int element;
            StringBuffer buf = new StringBuffer();

            // Iterate through the elements of the collator
            CollationElementIterator iter = collator.getCollationElementIterator(str);
            
            while ((element = iter.next()) != CollationElementIterator.NULLORDER)
            {
                // Generate a hexadecimal string representation of the Collation element
                // This can then be compared in a text sort ;-)
                String test = Integer.toString(element, 16);
                buf.append(test);
            }
            
            return buf.toString();
        }

        return str;
    }

    /**
     * We don't need to use the language parameter, so map this to
     * the standard sort string filter
     * @param str string
     * @param lang language
     * @return string
     */
    @Override
    public String filter(String str, String lang)
    {
        return filter(str);
    }
    
    /**
     * Get a Locale dependent collator
     * 
     * @return The collator to use
     */
    private static RuleBasedCollator getCollator()
    {
        // Get the Locale to use
        Locale locale = getSortLocale();
        
        if (locale != null)
        {
            // Get collator for the supplied Locale
            RuleBasedCollator collator = (RuleBasedCollator)Collator.getInstance(locale);
            
            if (collator != null)
            {
                return collator;
            }
        }
        return null;
    }
    
    /**
     * Get a Locale to use for the sorting
     * 
     * @return The Locale to use
     */
    private static Locale getSortLocale()
    {
        Locale theLocale = null;
        
        // Get a Locale configuration from the dspace.cfg
        String locale = ConfigurationManager.getProperty("webui.browse.sort.locale");
        
        if (locale != null)
        {
            // Attempt to create Locale for the configured value
            String[] localeArr = locale.split("_");

            if (localeArr.length > 1)
            {
                theLocale = new Locale(localeArr[0], localeArr[1]);
            }
            else
            {
                theLocale = new Locale(locale);
            }
            
            // Return the configured locale, or English default
            if (theLocale == null)
            {
                log.warn("Could not create the supplied Locale: webui.browse.sort.locale=" + locale);
                return Locale.ENGLISH;
            }
        }
        else
        {
            return Locale.ENGLISH;
        }

        return theLocale;
    }
}
