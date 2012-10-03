/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

/**
 * Configuration for one field that is to be highlighted
 * Giving 0 as max chars ensures that the entire field is returned !
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DiscoverHitHighlightingField {

    public static final int UNLIMITED_FRAGMENT_LENGTH = 0;

    private String field;
    private int maxChars;
    private int maxSnippets;

    public DiscoverHitHighlightingField(String field, int maxChars, int maxSnippets)
    {
        this.field = field;
        this.maxChars = maxChars;
        this.maxSnippets = maxSnippets;
    }

    public String getField()
    {
        return field;
    }

    /**
     * The max number of characters that should be shown for a
     * field containing a matching hit. e.g. If maxChars = 200
     * and a hit is found in the full-text the 200 chars
     * surrounding the hit will be shown
     */
    public int getMaxChars()
    {
        return maxChars;
    }

    public int getMaxSnippets()
    {
        return maxSnippets;
    }
}
