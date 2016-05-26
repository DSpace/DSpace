/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.components.flow.WebContinuation;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.objectmanager.DSpaceObjectManager;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.AbstractWingTransformer;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.UserMeta;
import org.dspace.app.xmlui.wing.ObjectManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.xml.sax.SAXException;

/**
 * @author Scott Phillips
 */
public abstract class AbstractDSpaceTransformer extends AbstractWingTransformer
        implements DSpaceTransformer
{

    private static final String NAME_TRIM = "org.dspace.app.xmlui.";

    protected Map objectModel;

    protected Context context;

    protected String contextPath;

    protected String servletPath;

    protected String sitemapURI;

    protected String url;
    
    protected Parameters parameters;
    
    protected EPerson eperson;
    
    protected WebContinuation knot;
    
    // Only access this through getObjectManager, so that we don't have to create one if we don't want to.
    private ObjectManager objectManager;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        this.objectModel = objectModel;
        this.parameters = parameters;
        try
        {
            this.context = ContextUtil.obtainContext(objectModel);
            this.eperson = context.getCurrentUser();
            Request request = ObjectModelHelper.getRequest(objectModel);
            this.contextPath = request.getContextPath();
            if (contextPath == null)
            {
                contextPath = "/";
            }
            
            this.servletPath = request.getServletPath();
            this.sitemapURI = request.getSitemapURI(); 
            this.knot = FlowHelper.getWebContinuation(objectModel);
        }
        catch (SQLException sqle)
        {
            handleException(sqle);
        }
        
        // Initialize the Wing framework.
        try
        {
            this.setupWing();
        }
        catch (WingException we)
        {
            throw new ProcessingException(we);
        }
    }

    @Override
    protected void handleException(Exception e) throws SAXException
    {
        throw new SAXException(
                "An error was encountered while processing the '"+this.getComponentName()+"' Wing based component: "
                        + this.getClass().getName(), e);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException, ProcessingException
    {
        // Do nothing
    }

    @Override
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        // Do nothing
    }

    @Override
    public void addUserMeta(UserMeta userMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Do nothing
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Do nothing
    }
    
    @Override
    public ObjectManager getObjectManager() 
    {
        if (this.objectManager == null)
        {
            this.objectManager = new DSpaceObjectManager();
        }
        return this.objectManager;
    }
    
    @Override
    public String getComponentName()
    {
        String name = this.getClass().getName();
        if (name.startsWith(NAME_TRIM))
        {
            name = name.substring(NAME_TRIM.length());
        }
        return name;
    }

    /**
     * Encode the given string for URL transmission.
     * 
     * @param unencodedString
     *            The unencoded string.
     * @return The encoded string
     * @throws org.dspace.app.xmlui.utils.UIException if the encoding is unsupported.
     */
    public static String encodeForURL(String unencodedString) throws UIException
    {
    	if (unencodedString == null)
        {
            return "";
        }

        try
        {
            return URLEncoder.encode(unencodedString,Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new UIException(uee);
        }

    }

    /**
     * Decode the given string from URL transmission.
     * 
     * @param encodedString
     *            The encoded string.
     * @return The unencoded string
     * @throws org.dspace.app.xmlui.utils.UIException if the encoding is unsupported.
     */
    public static String decodeFromURL(String encodedString) throws UIException
    {
    	if (encodedString == null)
        {
            return null;
        }
    	
        try
        {
            // Percent(%) is a special character, and must first be escaped as %25
            if (encodedString.contains("%"))
            {
                encodedString = encodedString.replace("%", "%25");
            }

            return URLDecoder.decode(encodedString, Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new UIException(uee);
        }

    }

    /**
     * Generate a URL for the given base URL with the given parameters. This is
     * a convenience method to make it easier to generate URL references with
     * parameters.
     * 
     * <p>Example:
     *
     * <pre>{@code
     * Map<String,String> parameters = new Map<String,String>();
     * parameters.put("arg1","value1");
     * parameters.put("arg2","value2");
     * parameters.put("arg3","value3");
     * String url = genrateURL("/my/url",parameters);
     * }</pre>
     *
     * would result in the string:
     *
     * <pre>{@code url == "/my/url?arg1=value1&arg2=value2&arg3=value3"}</pre>
     * 
     * @param baseURL The baseURL without any parameters.
     * @param parameters The parameters to be encoded on in the URL.
     * @return The parameterized Post URL.
     */
    public static String generateURL(String baseURL,
            Map<String, String> parameters)
    {
        StringBuilder urlBuffer = new StringBuilder();
        for (Map.Entry<String, String> param : parameters.entrySet())
        {
            if (urlBuffer.length() == 0)
            {
                urlBuffer.append(baseURL).append('?');
            }
            else
            {
                urlBuffer.append( '&');
            }

            urlBuffer.append(param.getKey()).append("=").append(param.getValue());
        }

        return urlBuffer.length() > 0 ? urlBuffer.toString() : baseURL;
    }
    
    
    @Override
    public void recycle() {
    	this.objectModel = null;
        this.context = null;
        this.contextPath = null;
        this.servletPath = null;
        this.sitemapURI = null;
        this.url=null;
        this.parameters=null;
        this.eperson=null;
        this.knot=null;
        this.objectManager=null;
    	super.recycle();
    }

    @Override
    public void dispose() {
    	this.objectModel = null;
        this.context = null;
        this.contextPath = null;
        this.servletPath = null;
        this.sitemapURI = null;
        this.url=null;
        this.parameters=null;
        this.eperson=null;
        this.knot=null;
        this.objectManager=null;
    	super.dispose();
    }
    
}
