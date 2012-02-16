package org.dspace.content.authority;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 7-dec-2010
 * Time: 15:45:55
 */
public interface SolrChoiceAuthority extends ChoiceAuthority{

    public Choices getMatches(String field, String text, int collection, int start, int limit, String locale, boolean bestMatch);

    public Choices getMatches(String field, String text, int collection, int start, int limit, String locale);

    public Choices getBestMatch(String fieldKey, String query, int collection, String locale);
}
