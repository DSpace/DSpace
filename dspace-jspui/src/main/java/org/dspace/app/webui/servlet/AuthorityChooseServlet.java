/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/*
 *
 */

package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.xml.sax.SAXException;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.ChoicesXMLGenerator;
import org.dspace.core.Context;

import org.apache.xml.serializer.SerializerFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Method;



/**
 *
 * @author bollini
 */
public class AuthorityChooseServlet extends DSpaceServlet {

    @Override
    protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
        process(context, request, response);
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
        process(context, request, response);
    }

    /**
     * Generate the AJAX response Document.
     *
     * Looks for request parameters:
     *  field - MD field key, i.e. form key, REQUIRED - derivated from url.
     *  query - string to match
     *  collection - db ID of Collection ot serve as context
     *  start - index to start from, default 0.
     *  limit - max number of lines, default 1000.
     *  format - opt. result XML/XHTML format: "select", "ul", "xml"(default)
     *  locale - explicit locale, pass to choice plugin
     */
    private void process(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
        String[] paths = request.getPathInfo().split("/");
        String field = paths[paths.length-1];
        ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();

        String query = request.getParameter("query");
        String format = request.getParameter("format");
        int collection = UIUtil.getIntParameter(request, "collection");
        int start = UIUtil.getIntParameter(request, "start");
        int limit = UIUtil.getIntParameter(request, "limit");

        Choices result = cam.getMatches(field, query, collection, start, limit, null);
//        Choice[] testValues = {
//            new Choice("rp0001", "VALUE1","TEST LABEL1"),
//            new Choice("rp0002", "VALUE2","TEST LABEL2"),
//            new Choice("rp0003", "VALUE3","TEST LABEL3"),
//            new Choice("rp0004", "VALUE COGN, LABEL1","TEST COGN, LABEL1"),
//            new Choice("rp0005", "VALUE COGN, LABEL2","TEST COGN, LABEL2"),
//            new Choice("rp0006", "VALUE COGN, LABEL3","TEST COGN, LABEL3")
//        };
//
//        Choices result = new Choices(testValues,start,testValues.length,Choices.CF_ACCEPTED,false);
        response.setContentType("text/xml; charset=\"utf-8\"");
        Writer writer = response.getWriter();
        // borrow xalan's serializer to let us use SAX choice menu generator
        Properties props =
           OutputPropertiesFactory.getDefaultMethodProperties(Method.XML);
        Serializer ser = SerializerFactory.getSerializer(props);
        ser.setWriter(writer);
        try
        {
            ChoicesXMLGenerator.generate(result, format, ser.asContentHandler());
        }
        catch(SAXException e)
        {
            throw new IOException(e.toString(), e);
        }
        finally
        {
            ser.reset();
        }
        writer.flush();
    }
}
