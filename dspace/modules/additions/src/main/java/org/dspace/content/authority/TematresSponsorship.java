/**
 * 170913 - Dan Shinkai
 * Classe desenvolvida baseada na classe SHERPARMEOJournal para carregar dados a partir do Tematres.
 */
package org.dspace.content.authority;

import org.apache.commons.httpclient.NameValuePair;

/**
 *  As informacoes nos quais serao recuperadas do Tematres deverao ser especificadas nas variaveis RESULT, LABEL e AUTHORITY.
 *  O RESULT determina a tag contendo todas as informacoes de uma determinada busca. 
 *  O LABEL e a tag no qual estara a informacoes a ser apresentada na pagina.
 *  O AUTHORITY esta relacionado com o authority propriamente dito.
 */
public class TematresSponsorship extends TematresProtocol
{
    private static final String RESULT = "term";
    private static final String LABEL = "string";
    private static final String AUTHORITY = "term_id";
        

    public TematresSponsorship()
    {
        super();
    }

    public Choices getMatches(String text, int collection, int start, int limit, String locale)
    {
        // punt if there is no query text
        if (text == null || text.trim().length() == 0)
        {
            return new Choices(true);
        }

        // query args to add to Tematres request URL
        NameValuePair args[] = new NameValuePair[2];
        args[0] = new NameValuePair("arg", text);
        args[1] = new NameValuePair("task","search"); // OR: starts, exact

        Choices result = query(RESULT, LABEL, AUTHORITY, args, start, limit);
        if (result == null)
        {
                        result =  new Choices(true);
        }
        return result;
    }

    @Override
    public Choices getMatches(String field, String text, int collection, int start, int limit, String locale) {
        return getMatches(text, collection, start, limit, locale);
    }
}