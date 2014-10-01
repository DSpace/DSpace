/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.AuthorProfile;

import java.util.Map;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DeleteAuthorAction extends AbstractAction {
    /**
     * Controls the processing against some values of the
     * <code>Dictionary</code> objectModel and returns a
     * <code>Map</code> object with values used in subsequent
     * sitemap substitution patterns.
     * <p/>
     * NOTE: This interface is designed so that implentations can be <code>ThreadSafe<code>.
     * When an action is ThreadSafe, only one instance serves all requests : this
     * reduces memory usage and avoids pooling.
     *
     * @param resolver    The <code>SourceResolver</code> in charge
     * @param objectModel The <code>Map</code> with object of the
     *                    calling environment which can be used
     *                    to select values this controller may need
     *                    (ie Request, Response).
     * @param source      A source <code>String</code> to the Action
     * @param parameters  The <code>Parameters</code> for this invocation
     * @return Map        The returned <code>Map</code> object with
     *         sitemap substitution values which can be used
     *         in subsequent elements attributes like src=
     *         using a xpath like expression: src="mydir/{myval}/foo"
     *         If the return value is null the processing inside
     *         the <map:act> element of the sitemap will
     *         be skipped.
     * @throws Exception Indicates something is totally wrong
     */
    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request req=ObjectModelHelper.getRequest(objectModel);

        if(req.getParameters().containsKey("delete")){
            AuthorProfile authorProfile= AuthorProfile.find(ContextUtil.obtainContext(objectModel), Integer.parseInt(req.getParameter("authorProfileId")));
            authorProfile.delete();
            redirector.globalRedirect(true,req.getContextPath()+"/browse-by");

        }
        return EMPTY_MAP;
    }
}
