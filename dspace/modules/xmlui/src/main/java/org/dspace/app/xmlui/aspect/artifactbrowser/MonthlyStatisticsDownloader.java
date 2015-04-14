/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
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
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.xml.sax.SAXException;

/**
 * Provides the usage statistics in CSV form
 *
 * @author Peter Dietz (pdietz84@gmail.com)
 */
public class MonthlyStatisticsDownloader extends AbstractReader implements
        Recyclable
{
    protected static final Logger log = Logger
            .getLogger(MonthlyStatisticsDownloader.class);

    private static SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd");

    protected Response response;

    protected Request request;

    protected Context context;

    protected PrintWriter writer = null;

    /**
     * DRUM Customizations for Monthly Statistics
     */
    private static final String strDspace = ConfigurationManager
            .getProperty("dspace.dir");

    private static final File dir = new File(strDspace + "/stats/monthly");

    @Override
    public void setup(SourceResolver sourceResolver, Map objectModel,
            String src, Parameters parameters) throws IOException,
            SAXException, ProcessingException
    {
        log.info("CSV Writer for embargo-list");
        super.setup(sourceResolver, objectModel, src, parameters);

        this.response = ObjectModelHelper.getResponse(objectModel);
        this.request = ObjectModelHelper.getRequest(objectModel);
        String requestURI = request.getRequestURI();
        String[] uriSegments = requestURI.split("/");
        String strMonth = uriSegments[uriSegments.length - 1];

        try
        {
            String strFile = dir.toString() + "/" + strMonth + "_stats.txt";
            BufferedReader br = new BufferedReader(new FileReader(strFile));
            writer = new PrintWriter(response.getWriter());
            response.setContentType("text/plain; encoding='UTF-8'");
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Content-Disposition", "attachment; filename="
                    + strMonth + "_stats.txt");
            String readString = null;
            while ((readString = br.readLine()) != null)
            {
                writer.println(readString);
            }
            writer.close();
        }
        catch (FileNotFoundException e)
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

    }

    @Override
    public void generate() throws IOException
    {
        log.info("Statistics file outputter");
        out.flush();
        out.close();
    }

    @Override
    public void recycle()
    {
        this.request = null;
        this.response = null;
    }

}
