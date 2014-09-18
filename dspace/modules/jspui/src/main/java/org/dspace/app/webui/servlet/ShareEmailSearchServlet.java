package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;



/**
 * Handle request for search sharings
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
public class ShareEmailSearchServlet extends DSpaceServlet
{

	
	private static final String SHARE_SEARCH_FILE_NAME = "share_search";
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ShareEmailSearchServlet.class);
	
	
	
	/**
	 * @see DSpaceServlet#doPost(HttpServletRequest, HttpServletResponse)
	 */
	@Override
	protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException, SQLException, AuthorizeException 
	{
		if (preventSpam(request)) 
		{
			/** Get associated parameters **/
			String sendersName = request.getParameter("senderName");
			String senderEmail = request.getParameter("senderEmail");
			String destination = request.getParameter("email");
			String bodyContent = request.getParameter("emailContent");
			String urlToShare = request.getParameter("urlToShare");

			if (sendersName != null && !senderEmail.isEmpty() && destination != null && !destination.isEmpty()) 
			{
				try 
				{
					Email email = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), SHARE_SEARCH_FILE_NAME));
					email.addRecipient(destination);
					email.addArgument(sendersName);
					email.addArgument(urlToShare);
					email.addArgument(bodyContent);
	
					/** "reply-to" **/
					if (senderEmail != null && !senderEmail.isEmpty()) 
					{
						email.setReplyTo(senderEmail);
					}
	
					email.send();
					
					request.setAttribute("has-error", false);
					request.setAttribute("message", "share.search.email.send.success");
					JSPManager.showJSP(request, response, "/search/share-search-result.jsp");
				} 
				catch (Exception e) 
				{
					logger.error(e.getMessage(), e);
					request.setAttribute("has-error", true);
					request.setAttribute("message", "share.search.email.send.error");
					JSPManager.showJSP(request, response, "/search/share-search-result.jsp");

				}
			} 
			else 
			{
				/** Not all required parameters have been filled **/
				request.setAttribute("has-error", true);
				request.setAttribute("message", "share.search.email.send.required");
				JSPManager.showJSP(request, response, "/search/share-search-result.jsp");
			}
		}
		else
		{
			logger.warn("Foi identificada tentativa de uso da ferramenta de envio de e-mails para spam");
			request.setAttribute("has-error", true);
			request.setAttribute("message", "share.search.email.send.spam");
			JSPManager.showJSP(request, response, "/search/share-search-result.jsp");
		}
	}


	/**
	 * Do spam preventing, same as disposed in {@link SuggestServlet#doDSGet(Context, HttpServletRequest, HttpServletResponse)}
	 * @param request Current HTTP request
	 * @throws UnknownHostException
	 * @throws AuthorizeException
	 */
	private boolean preventSpam(HttpServletRequest request) throws UnknownHostException, AuthorizeException 
	{
		String fromPage = request.getHeader("Referer");

        // Prevent spammers and splogbots from poisoning the feedback page
        String host = ConfigurationManager.getProperty("dspace.hostname");

        String basicHost = "";
        if (host.equals("localhost") || host.equals("127.0.0.1")
                || host.equals(InetAddress.getLocalHost().getHostAddress()))
        {
            basicHost = host;
        }
        else
        {
            // cut off all but the hostname, to cover cases where more than one URL
            // arrives at the installation; e.g. presence or absence of "www"
            int lastDot = host.lastIndexOf('.');
            basicHost = host.substring(host.substring(0, lastDot).lastIndexOf("."));
        }

        if (fromPage == null || fromPage.indexOf(basicHost) == -1)
        {
            return false;
        }
        
        return true;
	}

}
