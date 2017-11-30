/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.json;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

public abstract class JSONRequest
{
    private String subPath;

    public abstract void doJSONRequest(Context context, HttpServletRequest req,
            HttpServletResponse resp) throws AuthorizeException, IOException;

    public String getSubPath()
    {
        return subPath;
    }

    /**
     * Set the subPath that this plugin-instance will serve
     * @param subPath
     */
    public void setSubPath(String subPath)
    {
        this.subPath = subPath;
    }
}
