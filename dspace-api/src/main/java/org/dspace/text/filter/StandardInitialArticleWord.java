/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.text.filter;

/**
 * Implements existing DSpace initial article word behaviour
 * 
 * Note: This only works for languages defined with ISO code entries.
 * 
 * @author Graham Triggs
 */
public class StandardInitialArticleWord extends InitialArticleWord
{
    private static final String[] articleWords = { "the", "an", "a" };

    @Override
    protected String[] getArticleWords(String lang)
    {
        if (lang != null && lang.startsWith("en"))
        {
            return articleWords;
        }
        
        return null;
    }

}
 	  	 
