/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.NewsService;

/**
 * Servlet for editing the front page news
 *
 * @author gcarpent
 */
public class NewsEditServlet extends DSpaceServlet
{
	private final transient NewsService newsService
             = CoreServiceFactory.getInstance().getNewsService();
	
    @Override
    protected void doDSGet(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        //always go first to news-main.jsp
        JSPManager.showJSP(request, response, "/dspace-admin/news-main.jsp");
    }

    @Override
    protected void doDSPost(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        //Get submit button
        String button = UIUtil.getSubmitButton(request, "submit");

        String news = "";

        //Are we editing the top news or the sidebar news?
        String position = request.getParameter("position");

        if (button.equals("submit_edit"))
        {
            //get the existing text from the file
            news = newsService.readNewsFile(position);

            //pass the position back to the JSP
            request.setAttribute("position", position);

            //pass the existing news back to the JSP
            request.setAttribute("news", news);

            //show news edit page
            JSPManager
                    .showJSP(request, response, "/dspace-admin/news-edit.jsp");
        }
        else if (button.equals("submit_save"))
        {
            //get text string from form
            news = (String) request.getParameter("news");

            //write the string out to file
            newsService.writeNewsFile(position, news);

            JSPManager
                    .showJSP(request, response, "/dspace-admin/news-main.jsp");
        }
        else
        {
            //the user hit cancel, so return to the main news edit page
            JSPManager
                    .showJSP(request, response, "/dspace-admin/news-main.jsp");
        }

        c.complete();
    }
}
