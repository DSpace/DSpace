/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.handle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;


/**
 *
 * @author Pascal-Nicolas Becker (p dot becker at tu hyphen berlin dot de)
 */
public class HandleResolverReader extends AbstractReader implements Recyclable {
    
    private static final Logger log = Logger.getLogger(HandleResolverReader.class);
    
    public static final String CONTENTTYPE = "application/json; charset=utf-8";

    private Response resp;
    private String action;
    private String handle;
    private String prefix;
    
    
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException,
            IOException
    {
        this.resp = ObjectModelHelper.getResponse(objectModel);
        this.action = par.getParameter("action", "listprefixes");
        this.handle = par.getParameter("handle", null);
        this.prefix = par.getParameter("prefix", null);

        super.setup(resolver, objectModel, src, par);
    }
    
    @Override
    public void generate() throws IOException, SAXException, ProcessingException {
        Context context = null;
        try {
            context = ContextUtil.obtainContext(objectModel);
        } catch (SQLException ex) {
            log.error(ex);
            throw new ProcessingException("Error in database conncetion.", ex);
        }
        
        Gson gson = new Gson();
        String jsonString = null;

        try {
            if (action.equals("resolve"))
            {
                if (StringUtils.isBlank(handle))
                {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                String url = HandleManager.resolveToURL(context, handle);
                // Only an array or an abject is valid JSON. A simple string
                // isn't. An object always uses key value pairs, so we use an
                // array.
                if (url != null)
                {
                    jsonString = gson.toJson(new String[] {url});
                }
                else
                {
                    jsonString = gson.toJson(null);
                }
            }
            else if (action.equals("listprefixes"))
            {
                List<String> prefixes = new ArrayList<String>();
                prefixes.add(HandleManager.getPrefix());
                String additionalPrefixes = ConfigurationManager
                        .getProperty("handle.additional.prefixes");
                if (StringUtils.isNotBlank(additionalPrefixes))
                {
                    for (String apref : additionalPrefixes.split(","))
                    {
                        prefixes.add(apref.trim());
                    }
                }
                jsonString = gson.toJson(prefixes);
            }
            else if (action.equals("listhandles"))
            {
                if (ConfigurationManager.getBooleanProperty(
                        "handle.hide.listhandles", true))
                {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                
                if (StringUtils.isBlank(prefix))
                {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                List<String> handlelist = HandleManager.getHandlesForPrefix(
                        context, prefix);
                String[] handles = handlelist.toArray(new String[handlelist.size()]);
                jsonString = gson.toJson(handles);
            }
        } catch (SQLException e) {
            log.error("SQLException: ", e);
            return;
        }
        
        try {
            ObjectModelHelper.getResponse(objectModel).setHeader("Content-Type", CONTENTTYPE);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
            IOUtils.copy(inputStream, out);
            out.flush();
        } catch (Exception e) {
            log.error("Error: ", e);
        }
    }
    
    public void recycle() {
        this.resp = null;
        this.action = null;
        this.handle = null;
        this.prefix = null;
        super.recycle();
    }
}