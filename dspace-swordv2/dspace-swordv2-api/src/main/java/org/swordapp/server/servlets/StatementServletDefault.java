/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.swordapp.server.servlets;

import org.swordapp.server.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.log4j.Logger;

public class StatementServletDefault extends SwordServlet
{
    private static Logger log = Logger.getLogger(StatementServletDefault.class);

    private StatementManager sm;
    private StatementAPI statementApi;

    public void init() throws ServletException
    {
		super.init();
		
        // load the container manager implementation
        this.sm = (StatementManager) this.loadImplClass("statement-impl", false);

        // initialise the underlying servlet processor
        this.statementApi = new StatementAPI(this.sm, this.config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        this.statementApi.get(req, resp);
    }
}
