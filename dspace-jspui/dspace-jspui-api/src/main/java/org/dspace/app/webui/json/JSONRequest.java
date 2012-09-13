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
