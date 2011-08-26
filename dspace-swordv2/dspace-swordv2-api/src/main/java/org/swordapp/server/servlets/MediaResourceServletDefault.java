package org.swordapp.server.servlets;

import org.apache.log4j.Logger;
import org.swordapp.server.MediaResourceAPI;
import org.swordapp.server.MediaResourceManager;
import org.swordapp.server.SwordConfiguration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MediaResourceServletDefault extends SwordServlet
{
    private static Logger log = Logger.getLogger(MediaResourceServletDefault.class);

    protected MediaResourceManager mrm;
    protected MediaResourceAPI api;

    public void init() throws ServletException
    {
		super.init();

        // load the Media Resource Manager
        this.mrm = (MediaResourceManager) this.loadImplClass("media-resource-impl", false);

        // load the api
        this.api = new MediaResourceAPI(this.mrm, this.config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        this.api.get(req, resp);
    }

	@Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        this.api.head(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        this.api.post(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        this.api.put(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        this.api.delete(req, resp);
    }
}
