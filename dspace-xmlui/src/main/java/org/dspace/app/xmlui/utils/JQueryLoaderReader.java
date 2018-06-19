/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.reading.AbstractReader;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;

import java.io.IOException;

/**
 * Loads in the jquery javascript library if it hasn't been loaded in already
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class JQueryLoaderReader extends AbstractReader {

    @Override
    public void generate() throws IOException, SAXException, ProcessingException {
        String contextPath = ObjectModelHelper.getRequest(objectModel).getContextPath();

        String script = "!window.jQuery && document.write('<script type=\"text/javascript\" src=\"" + contextPath + "/static/js/jquery-1.6.4.min.js\">&nbsp;</script>');";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(script.getBytes("UTF-8"));

        byte[] buffer = new byte[8192];

        Response response = ObjectModelHelper.getResponse(objectModel);
        response.setHeader("Content-Length", String.valueOf(script.length()));
        response.setHeader("Content-Type", "text/javascript");
        int length;
        while ((length = inputStream.read(buffer)) > -1)
        {
            out.write(buffer, 0, length);
        }
        out.flush();
        out.close();
    }
}
