package org.dspace.app.xmlui.cocoon;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.matching.modular.WildcardMatcher;
import org.apache.cocoon.sitemap.PatternException;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.uri.IdentifierFactory;
import org.dspace.uri.ResolvableIdentifier;

import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Richard Jones
 */
public class IdentifierMatcher extends WildcardMatcher
{
    public Map match(String s, Map objectModel, Parameters parameters) throws PatternException
    {

        try
        {
            Context context = ContextUtil.obtainContext(objectModel);
            ResolvableIdentifier ri = IdentifierFactory.resolve(context, s);

            // if no identifier is resolved, then return null, and a page-not-found will be shown
            if (ri == null)
            {
                return null;
            }

            // store the results
            Map<String, String> result = new HashMap<String, String>();
            result.put("identifierType", ri.getIdentifierType());

            // find out if the parent matcher implementation has anything to say
            String extra = s.substring(s.indexOf(ri.getURLForm()) + ri.getURLForm().length());
            Map smap = super.match(extra, objectModel, parameters);

            if (smap == null)
            {
                return null;
            }

            // add the result sets together
            result.putAll(smap);

            return result;
        }
        catch (SQLException e)
        {
            // FIXME:???
            return null;
        }
    }
}
