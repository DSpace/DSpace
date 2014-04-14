/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dspace.identifier.doi.DOIIdentifierException;

/**
 * DOI identifiers.
 *
 * @author Pascal-Nicolas Becker
 */
public class DOI
        implements Identifier
{
    public static final String SCHEME = "doi:";

    public static final String RESOLVER = "http://dx.doi.org";
    
    
    /**
     * This method helps to convert a DOI into a URL. It takes DOIs in one of
     * the following formats  and returns it as URL (f.e. 
     * http://dx.doi.org/10.123/456). Allowed formats are:
     * <ul>
     *   <li>doi:10.123/456</li>
     *   <li>10.123/456</li>
     *   <li>http://dx.doi.org/10.123/456</li>
     * </ul>
     * 
     * @param identifier  A DOI that should be returned in external form.
     * @return A String containing a URL to the official DOI resolver.
     * @throws IllegalArgumentException If identifier is null or an empty String.
     * @throws IdentifierException If identifier could not be recognized as valid DOI.
     */
    public static String DOIToExternalForm(String identifier)
            throws IdentifierException
    {
        if (null == identifier) 
            throw new IllegalArgumentException("Identifier is null.", new NullPointerException());
        if (identifier.isEmpty())
            throw new IllegalArgumentException("Cannot format an empty identifier.");
        if (identifier.startsWith(SCHEME))
            return RESOLVER + "/" + identifier.substring(SCHEME.length());
        if (identifier.startsWith("10.") && identifier.contains("/"))
            return RESOLVER + "/" + identifier;
        if (identifier.startsWith(RESOLVER + "/10."))
            return identifier;
        
        throw new IdentifierException(identifier + "does not seem to be a DOI.");
    }
    
    public static String DOIFromExternalFormat(String identifier)
            throws DOIIdentifierException
    {
        Pattern pattern = Pattern.compile("^" + RESOLVER + "/+(10\\..*)$");
        Matcher matcher = pattern.matcher(identifier);
        if (matcher.find())
        {
            return SCHEME + matcher.group(1);
        }

        throw new DOIIdentifierException("Cannot recognize DOI!",
                DOIIdentifierException.UNRECOGNIZED);
    }

    /**
     * Recognize format of DOI and return it with leading doi-Scheme.
     * @param identifier Identifier to format, following format are accepted:
     *                   f.e. 10.123/456, doi:10.123/456, http://dx.doi.org/10.123/456.
     * @return Given Identifier with DOI-Scheme, f.e. doi:10.123/456.
     * @throws IllegalArgumentException If identifier is empty or null.
     * @throws DOIIdentifierException If DOI could not be recognized.
     */
    public static String formatIdentifier(String identifier)
            throws DOIIdentifierException
    {
        if (null == identifier) {
            throw new IllegalArgumentException("Identifier is null.", new NullPointerException());
        }
        if (identifier.startsWith(DOI.SCHEME)) {
            return identifier;
        }
        if (identifier.isEmpty()) {
            throw new IllegalArgumentException("Cannot format an empty identifier.");
        }
        if (identifier.startsWith("10.") && identifier.contains("/")) {
            return DOI.SCHEME + identifier;
        }
        if (identifier.startsWith(RESOLVER + "/10.")) {
            return DOI.SCHEME + identifier.substring(18);
        }
        throw new DOIIdentifierException(identifier + "does not seem to be a DOI.",
                DOIIdentifierException.UNRECOGNIZED);
    }
}
