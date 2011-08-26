package org.swordapp.server.servlets;

import org.swordapp.server.ServiceDocumentAPI;
import org.swordapp.server.ServiceDocumentManager;
import org.swordapp.server.SwordConfiguration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServiceDocumentServletDefault extends SwordServlet
{
    protected ServiceDocumentManager sdm;
    protected ServiceDocumentAPI api;

    public void init() throws ServletException
    {
		super.init();

        // load the service document implementation
        this.sdm = (ServiceDocumentManager) this.loadImplClass("service-document-impl", false);

        // load the api
        this.api = new ServiceDocumentAPI(this.sdm, this.config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        this.api.get(req, resp);
    }
}
