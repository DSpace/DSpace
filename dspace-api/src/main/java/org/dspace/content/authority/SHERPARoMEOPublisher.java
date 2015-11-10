/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.message.BasicNameValuePair;
import org.dspace.content.Collection;

/**
 * Sample Publisher name authority based on SHERPA/RoMEO
 *
 *
 * WARNING: This is a very crude and incomplete implementation, done mainly
 *  as a proof-of-concept.  Any site that actually wants to use it will
 *  probably have to refine it (and give patches back to dspace.org).
 *
 * @see SHERPARoMEOProtocol
 * @author Larry Stone
 * @version $Revision $
 */
public class SHERPARoMEOPublisher extends SHERPARoMEOProtocol
{
    protected static final String RESULT = "publisher";
    protected static final String LABEL = "name";
    // note: the publisher records have nothing we can use as authority code.
    protected static final String AUTHORITY = null;

    public SHERPARoMEOPublisher()
    {
        super();
    }

    @Override
    public Choices getMatches(String text, Collection collection, int start, int limit, String locale)
    {
        // punt if there is no query text
        if (text == null || text.trim().length() == 0)
        {
            return new Choices(true);
        }

        // query args to add to SHERPA/RoMEO request URL
        List<BasicNameValuePair> args = new ArrayList<BasicNameValuePair>();
        args.add(new BasicNameValuePair("pub", text));
        args.add(new BasicNameValuePair("qtype","all")); // OR: starts, exact

        Choices result = query(RESULT, LABEL, AUTHORITY, args, start, limit);
        if (result == null)
        {
            result =  new Choices(true);
        }
        return result;
    }

    @Override
    public Choices getMatches(String field, String text, Collection collection, int start, int limit, String locale) {
        return getMatches(text, collection, start, limit, locale);
    }    
}
