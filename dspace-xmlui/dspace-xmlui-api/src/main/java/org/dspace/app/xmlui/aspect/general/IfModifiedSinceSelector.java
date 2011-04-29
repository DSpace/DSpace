/*
 * IfModifiedSinceSelector.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.xmlui.aspect.general;

import java.util.Map;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.selection.Selector;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;

/**
 * This simple selector looks for the If-Modified-Since header, and
 * returns true if the Item in the request has not been modified since that
 * date.  The expression is ignored since the test is inherent in the request.
 *
 * Typical sitemap usage:
 *
 *  <map:match type="HandleTypeMatcher" pattern="item">
 *    <map:select type="IfModifiedSinceSelector">
 *      <map:when test="true">
 *        <map:act type="NotModifiedAction"/>
 *        <map:serialize/>
 *      </map:when>
 *      <map:otherwise>
 *        <map:transform type="ItemViewer"/>
 *        <map:serialize type="xml"/>
 *      </map:otherwise>
 *    </map:select>
 *  </map:match>
 *
 * @author Larry Stone
 */
public class IfModifiedSinceSelector implements Selector
{

    private static Logger log = Logger.getLogger(IfModifiedSinceSelector.class);

    /**
     * Check for If-Modified-Since header on request,
     * and returns true if the Item should *not* be sent, i.e.
     * if the response status should be 304 (HttpServletResponse.SC_NOT_MODIFIED).
     *
     * @param expression is ignored
     * @param objectModel
     *            environment passed through via cocoon
     * @return null or map containing value of sitemap parameter 'pattern'
     */
    public boolean select(String expression, Map objectModel,
            Parameters parameters)
    {
        try
        {
            Request request = ObjectModelHelper.getRequest(objectModel);
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso.getType() == Constants.ITEM)
            {
                Item item = (Item) dso;
                long modSince = request.getDateHeader("If-Modified-Since");
                if (modSince != -1 && item.getLastModified().getTime() < modSince)
                {
                    return true;
                }
            }
            return false;
        }
        catch (Exception e)
        {
            log.error("Error selecting based on If-Modified-Since: "+e.toString());
            return false;
        }
    }
}
