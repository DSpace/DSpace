/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.general;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;

/**
 * Simple action to return a 304 (Not Modified) status, for when request
 * had the If-modified-since header and resource has not been modified.
 * Used in conjunction with IfModifiedSinceSelector
 *
 * @author Larry Stone
 */

public class NotModifiedAction extends AbstractAction
{
    /**
     * Return a 304 (Not Modified) status in the response.
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
        httpResponse.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        return new HashMap();
    }
}
