/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.util.*;
import org.apache.avalon.framework.parameters.*;
import org.apache.cocoon.acting.*;
import org.apache.cocoon.environment.*;
import org.dspace.app.xmlui.utils.*;

/**
 * This action is used to abort the context when an exception occurs.
 * The action is called by the handle-errors section in either the theme's sitemap or the
 * main webapp/sitemap if the theme does not have a handle-errors section.
 *
 * @author philip at atmire.com
 */
public class ContextAbortAction extends ServiceableAction {
    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        ContextUtil.abortContext(ObjectModelHelper.getRequest(objectModel));
        return EMPTY_MAP;
    }
}
