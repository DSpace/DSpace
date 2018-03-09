/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.jstl.core.Config;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.itemmarking.ItemMarkingExtractor;
import org.dspace.app.itemmarking.ItemMarkingInfo;
import org.dspace.app.util.Util;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.DOI;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.identifier.service.IdentifierService;

/**
 * Miscellaneous UI utility methods
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class UIUtil extends Util
{
    /** Whether to look for x-forwarded headers for logging IP addresses */
    private static Boolean useProxies;

    /** log4j category */
    public static final Logger log = Logger.getLogger(UIUtil.class);

    /**
	 * Pattern used to get file.ext from filename (which can be a path)
	 */
	private static Pattern p = Pattern.compile("[^/]*$");
	
        private static boolean initialized = false;
	    
	private static AuthenticationService authenticationService;
	private static EPersonService personService;
        private static IdentifierService identifierService;
        private static DOIService doiService;
        private static HandleService handleService;
	
	private static synchronized void initialize() {
		if (initialized) {
			return;
		}
		authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
                doiService = IdentifierServiceFactory.getInstance().getDOIService();
                handleService = HandleServiceFactory.getInstance().getHandleService();
                identifierService = IdentifierServiceFactory.getInstance().getIdentifierService();
		personService = EPersonServiceFactory.getInstance().getEPersonService();
	}

    /**
     * Obtain a new context object. If a context object has already been created
     * for this HTTP request, it is re-used, otherwise it is created. If a user
     * has authenticated with the system, the current user of the context is set
     * appropriately.
     *
     * @param request
     *            the HTTP request
     *
     * @return a context object
     */
    public static Context obtainContext(HttpServletRequest request)
            throws SQLException
    {
    	initialize();

        //Set encoding to UTF-8, if not set yet
        //This avoids problems of using the HttpServletRequest
        //in the getSpecialGroups() for an AuthenticationMethod,
        //which causes the HttpServletRequest to default to
        //non-UTF-8 encoding.
        try
        {
            if(request.getCharacterEncoding()==null)
            {
                request.setCharacterEncoding(Constants.DEFAULT_ENCODING);
            }
        }
        catch(Exception e)
        {
            log.error("Unable to set encoding to UTF-8.", e);
        }

        Context c = (Context) request.getAttribute("dspace.context");


        if (c == null)
        {
            // No context for this request yet
            c = new Context();
            HttpSession session = request.getSession();

            // See if a user has authentication
            UUID userID = (UUID) session.getAttribute(
                    "dspace.current.user.id");

            if (userID != null)
            {
                String remAddr = (String)session.getAttribute("dspace.current.remote.addr");
                if (remAddr != null && remAddr.equals(request.getRemoteAddr()))
                {
                	EPerson e = personService.find(c, userID);

                	Authenticate.loggedIn(c, request, e);
                }
                else
                {
                    log.warn("POSSIBLE HIJACKED SESSION: request from "+
                             request.getRemoteAddr()+" does not match original "+
                             "session address: "+remAddr+". Authentication rejected.");
                }
            }

            // Set any special groups - invoke the authentication mgr.
            List<Group> groups = authenticationService.getSpecialGroups(c, request);

            for (Group g : groups)
            {
                c.setSpecialGroup(g.getID());
                log.debug("Adding Special Group id="+g.getID().toString());
            }

            // Set the session ID and IP address
            String ip = request.getRemoteAddr();
            if (useProxies == null) {
                useProxies = ConfigurationManager.getBooleanProperty("useProxies", false);
            }
            if(useProxies && request.getHeader("X-Forwarded-For") != null)
            {
                /* This header is a comma delimited list */
	            for(String xfip : request.getHeader("X-Forwarded-For").split(","))
                {
                    if(!request.getHeader("X-Forwarded-For").contains(ip))
                    {
                        ip = xfip.trim();
                    }
                }
	        }
            c.setExtraLogInfo("session_id=" + request.getSession().getId() + ":ip_addr=" + ip);

            // Store the context in the request
            request.setAttribute("dspace.context", c);
        }

        // Set the locale to be used
        Locale sessionLocale = getSessionLocale(request);
        Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
        c.setCurrentLocale(sessionLocale);

        return c;
    }
    
     /**
     * Returns a string array containing the URL to address this Item in DSpace,
     * the official URL of its preferred identifier (example given
     * http://dx.doi.org/10.5072/123) and the preferred identifier in canonical
     * form (example given doi:10.5072/123). The configuration property 
     * webui.preferred.identifier can be used to configure which identifier 
     * should be preferred.
     * If no identifier is found this method returns null. If no handle but a
     * DOI is found the first value of the array is null.
     * 
     * @param ctx DSpace Context
     * @param item the item
     * @return string array containing URL or null if no ID found; string
               array contains null if no handle, but a DOI is found
     * @throws SQLException
     */
    public static String[] getItemIdentifier(Context ctx, Item item)
            throws SQLException
    {
        initialize();
        // look up the the version handle
        String versionHandle = item.getHandle();

        // lookup the version doi
        String versionDOI = identifierService.lookup(ctx, item, DOI.class);
        
        // resolve identifiers
        String[] handles = null;
        if (versionHandle != null)
        {
            handles = new String[] {
                handleService.resolveToURL(ctx, versionHandle),
                handleService.getCanonicalForm(versionHandle),
                versionHandle,
                "hdl:" + versionHandle
            };
        }
        String[] dois = null;
        if (versionDOI != null)
        {
            try {
                dois = new String[] {
                    handleService.resolveToURL(ctx, versionHandle),
                    doiService.DOIToExternalForm(versionDOI),
                    doiService.formatIdentifier(versionDOI).substring(DOI.SCHEME.length()),
                    doiService.formatIdentifier(versionDOI)
                };
            } catch (Exception ex) {
                dois = null;
                log.error("Unable to format DOI " + versionDOI 
                        + ". " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
        
        // do we prefer DOIs or handles?
        if (dois != null
                && ("doi".equalsIgnoreCase(ConfigurationManager.getProperty("webui.preferred.identifier")) 
                    || handles == null))
        {
            return dois;
        }
        
        return handles;
    }

    /**
     * Get the current community location, that is, where the user "is". This
     * returns <code>null</code> if there is no location, i.e. "all of DSpace"
     * is the location.
     *
     * @param request
     *            current HTTP request
     *
     * @return the current community location, or null
     */
    public static Community getCommunityLocation(HttpServletRequest request)
    {
    	initialize();
    	
        return ((Community) request.getAttribute("dspace.community"));
    }

    /**
     * Get the current collection location, that is, where the user "is". This
     * returns null if there is no collection location, i.e. the location is
     * "all of DSpace" or a community.
     *
     * @param request
     *            current HTTP request
     *
     * @return the current collection location, or null
     */
    public static Collection getCollectionLocation(HttpServletRequest request)
    {
    	initialize();
    	
        return ((Collection) request.getAttribute("dspace.collection"));
    }

    /**
     * Put the original request URL into the request object as an attribute for
     * later use. This is necessary because forwarding a request removes this
     * information. The attribute is only written if it hasn't been before; thus
     * it can be called after a forward safely.
     *
     * @param request
     *            the HTTP request
     */
    public static void storeOriginalURL(HttpServletRequest request)
    {
    	initialize();
    	
        String orig = (String) request.getAttribute("dspace.original.url");

        if (orig == null)
        {
            String fullURL = request.getRequestURL().toString();

            if (request.getQueryString() != null)
            {
                fullURL = fullURL + "?" + request.getQueryString();
            }

            request.setAttribute("dspace.original.url", fullURL);
        }
    }

    /**
     * Get the original request URL.
     *
     * @param request
     *            the HTTP request
     *
     * @return the original request URL
     */
    public static String getOriginalURL(HttpServletRequest request)
    {
    	initialize();
    	
        // Make sure there's a URL in the attribute
        storeOriginalURL(request);

        return ((String) request.getAttribute("dspace.original.url"));
    }

    /**
     * Write a human-readable version of a DCDate.
     *
     * @param d
     *            the date
     * @param time
     *            if true, display the time with the date
     * @param localTime
     *            if true, adjust for local timezone, otherwise GMT
     * @param request
     *            the servlet request
     *
     * @return the date in a human-readable form.
     */
    public static String displayDate(DCDate d, boolean time, boolean localTime, HttpServletRequest request)
    {
    	initialize();
    	
        return d.displayDate(time, localTime, getSessionLocale(request));
            }

    /**
     * Return a string for logging, containing useful information about the
     * current request - the URL, the method and parameters.
     *
     * @param request
     *            the request object.
     * @return a multi-line string containing information about the request.
     */
    public static String getRequestLogInfo(HttpServletRequest request)
    {
    	initialize();
    	
        StringBuilder report = new StringBuilder();

        report.append("-- URL Was: ").append(getOriginalURL(request)).append("\n").toString();
        report.append("-- Method: ").append(request.getMethod()).append("\n").toString();

        // First write the parameters we had
        report.append("-- Parameters were:\n");

        Enumeration e = request.getParameterNames();

        while (e.hasMoreElements())
        {
            String name = (String) e.nextElement();

            if (name.equals("login_password"))
            {
                // We don't want to write a clear text password
                // to the log, even if it's wrong!
                report.append("-- ").append(name).append(": *not logged*\n").toString();
            }
            else
            {
                report.append("-- ").append(name).append(": \"")
                        .append(request.getParameter(name)).append("\"\n");
            }
        }

        return report.toString();
    }


    /**
     * Get the Locale for a session according to the user's language selection or language preferences.
     * Order of selection
     * - language selected via UI
     * - language as set by application
     * - language browser default
     *
     * @param request
     *        the request Object
     * @return supportedLocale
     *         Locale supported by this DSpace Instance for this request
     */
    public static Locale getSessionLocale(HttpServletRequest request)

    {
    	initialize();
    	
        String paramLocale = request.getParameter("locale");
        Locale sessionLocale = null;
        Locale supportedLocale = null;

        if (!StringUtils.isEmpty(paramLocale))
        {
            /* get session locale according to user selection */
            sessionLocale = new Locale(paramLocale);
        }


        if (sessionLocale == null)
        {
            /* get session locale set by application */
            HttpSession session = request.getSession();
            sessionLocale = (Locale) Config.get(session, Config.FMT_LOCALE);
        }

        /*
         * if session not set by selection or application then default browser
         * locale
         */
        if (sessionLocale == null)
        {
            sessionLocale = request.getLocale();
        }

        if (sessionLocale == null)
        {
            sessionLocale = I18nUtil.DEFAULTLOCALE;
        }
        supportedLocale =  I18nUtil.getSupportedLocale(sessionLocale);

        return supportedLocale;
    }


    /**
     * Send an alert to the designated "alert recipient" - that is, when a
     * database error or internal error occurs, this person is sent an e-mail
     * with details.
     * <P>
     * The recipient is configured via the "alert.recipient" property in
     * <code>dspace.cfg</code>. If this property is omitted, no alerts are
     * sent.
     * <P>
     * This method "swallows" any exception that might occur - it will just be
     * logged. This is because this method will usually be invoked as part of an
     * error handling routine anyway.
     *
     * @param request
     *            the HTTP request leading to the error
     * @param exception
     *            the exception causing the error, or null
     */
    public static void sendAlert(HttpServletRequest request, Exception exception)
    {
    	initialize();
    	
        String logInfo = UIUtil.getRequestLogInfo(request);
        Context c = (Context) request.getAttribute("dspace.context");
        Locale locale = getSessionLocale(request);
        EPerson user = null;

        try
        {
            String recipient = ConfigurationManager
                    .getProperty("alert.recipient");

            if (StringUtils.isNotBlank(recipient))
            {
                Email email = Email.getEmail(I18nUtil.getEmailFilename(locale, "internal_error"));
                email.addRecipient(recipient);
                email.addArgument(ConfigurationManager
                        .getProperty("dspace.url"));
                email.addArgument(new Date());
                email.addArgument(request.getSession().getId());
                email.addArgument(logInfo);

                String stackTrace;

                if (exception != null)
                {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);
                    pw.flush();
                    stackTrace = sw.toString();
                }
                else
                {
                    stackTrace = "No exception";
                }

                email.addArgument(stackTrace);
                try
                {
                    user = c.getCurrentUser();
                }
                catch (Exception e)
                {
                    log.warn("No context, the database might be down or the connection pool exhausted.");
                }

                if (user != null)
                {
                    email.addArgument(user.getFullName() + " (" + user.getEmail() + ")");
                }
                else
                {
                    email.addArgument("Anonymous");
                }
                email.addArgument(request.getRemoteAddr());
                email.send();
            }
        }
        catch (Exception e)
        {
            // Not much we can do here!
            log.warn("Unable to send email alert", e);
        }
    }

    /**
	 * Evaluate filename and client and encode appropriate disposition
	 *
	 * @param filename
	 * @param request
	 * @param response
	 * @throws UnsupportedEncodingException
	 */
	public static void setBitstreamDisposition(String filename, HttpServletRequest request,
			HttpServletResponse response)
	{
		initialize();
		
		String name = filename;

		Matcher m = p.matcher(name);

		if (m.find() && !m.group().equals(""))
		{
			name = m.group();
		}

		try
		{
			String agent = request.getHeader("USER-AGENT");

			if (null != agent && -1 != agent.indexOf("MSIE"))
			{
				name = URLEncoder.encode(name, "UTF8");
			}
			else if (null != agent && -1 != agent.indexOf("Mozilla"))
			{
				name = MimeUtility.encodeText(name, "UTF8", "B");
			}

		}
		catch (UnsupportedEncodingException e)
		{
			log.error(e.getMessage(),e);
		}
		finally
		{
			response.setHeader("Content-Disposition", "attachment;filename=" + name);
		}
	}
	
	/**
	 * Generate the (X)HTML required to show the item marking. Based on the markType it tries to find
	 * the corresponding item marking Strategy on the item_marking.xml Spring configuration file in order
	 * to apply it to the item.
	 * This method is used in BrowseListTag and ItemListTag to du the actual item marking in browse
	 * and search results
	 * 
	 * @param hrq The servlet request
	 * @param dso The DSpaceObject to mark (it can be a BrowseItem or an Item)
	 * @param markType the type of the mark.
	 * @return (X)HTML markup
	 * @throws JspException
	 */
    public static String getMarkingMarkup(HttpServletRequest hrq, DSpaceObject dso, String markType)
            throws JspException
    {
    	initialize();
    	
    	try
    	{
            String contextPath = hrq.getContextPath();

            Context c = UIUtil.obtainContext(hrq);
            
            Item item = (Item) dso;

            String mark = markType.replace("mark_", "");
            
            ItemMarkingExtractor markingExtractor = DSpaceServicesFactory.getInstance().getServiceManager()
				.getServiceByName(
						ItemMarkingExtractor.class.getName()+"."+mark,
						ItemMarkingExtractor.class);
            
            if (markingExtractor == null){ // In case we cannot find the corresponding extractor (strategy) in xml beans
            	return "";
            }
            
            ItemMarkingInfo markInfo = markingExtractor.getItemMarkingInfo(c, item);
            
            if (markInfo == null){
            	return "";
            }
            
            StringBuffer markFrag = new StringBuffer();
            
            String localizedTooltip = null;
            if (markInfo.getTooltip()!=null){
            	localizedTooltip = org.dspace.core.I18nUtil.getMessage(markInfo.getTooltip(), hrq.getLocale());
            }
            		
            String markLink = markInfo.getLink();
            
            if (markInfo.getImageName()!=null){
            	
            	//Link
            	if (StringUtils.isNotEmpty(markLink)){
            		markFrag.append("<a href=\"")
            			.append(contextPath+"/" + markLink)
            			.append("\">");
            	}
            	
            	markFrag.append("<img class=\""+markType+"_img\" src=\""+ contextPath+"/")
            		.append(markInfo.getImageName()).append("\"");
            	if (StringUtils.isNotEmpty(localizedTooltip)){
            		markFrag.append(" title=\"")
            			.append(localizedTooltip)
            			.append("\"");
            	}
            	markFrag.append("/>");
            	
            	//Link
            	if (StringUtils.isNotEmpty(markLink)){
            		markFrag.append("</a>");
            	}
            }
            else  if (markInfo.getClassInfo()!=null){
            	//Link
            	if (StringUtils.isNotEmpty(markLink)){
            		markFrag.append("<a href=\"")
            			.append(contextPath+"/" + markLink)
            			.append("\">");
            	}

            	markFrag.append("<div class=\""+markType+"_class" + " " + markInfo.getClassInfo() + "\" ");
            	if (StringUtils.isNotEmpty(localizedTooltip)){
            		markFrag.append(" title=\"")
            		.append(localizedTooltip)
            		.append("\"");
            	}
            	markFrag.append("/>");

            	//Link
            	if (StringUtils.isNotEmpty(markLink)){
            		markFrag.append("</a>");
            	}
            }
            
        	return markFrag.toString();
        }
        catch (SQLException sqle)
        {
        	throw new JspException(sqle.getMessage(), sqle);
        }
    }
}
