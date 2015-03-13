/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.dspace.core.Context;
import org.dspace.storage.rdbms.TableRow;
import org.xml.sax.SAXException;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Provides the usage statistics in CSV form
 *
 * @author Peter Dietz (pdietz84@gmail.com)
 */
public class EmbargoListDownloader extends AbstractReader implements Recyclable
{
    protected static final Logger log = Logger
            .getLogger(EmbargoListDownloader.class);

    private static SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd");

    protected Response response;

    protected Request request;

    protected Context context;

    protected CSVWriter writer = null;

    @Override
    public void setup(SourceResolver sourceResolver, Map objectModel,
            String src, Parameters parameters) throws IOException,
            SAXException, ProcessingException
    {
        log.info("CSV Writer for embargo-list");
        super.setup(sourceResolver, objectModel, src, parameters);

        this.response = ObjectModelHelper.getResponse(objectModel);
        this.request = ObjectModelHelper.getRequest(objectModel);
        response.setContentType("text/csv; encoding='UTF-8'");
        response.setStatus(HttpServletResponse.SC_OK);
        writer = new CSVWriter(response.getWriter());
        response.setHeader("Content-Disposition",
                "attachment; filename=embargo-list.csv");

        try
        {
            addEmbargoDataToWriter(this.request);
        }
        catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void addEmbargoDataToWriter(Request request) throws SQLException
    {
        ArrayList<TableRow> rowList = (ArrayList<TableRow>) EmbargoListHelper
                .getEmbargoList(request);

        writer.writeNext(new String[] { "Handle", "Item ID", "Bitstream ID",
                "Title", "Advisor", "Author", "Department", "Type", "End Date" });

        if (rowList.size() == 0)
        {
            return;
        }

        for (TableRow row : rowList)
        {
            String[] entryData = new String[9];

            entryData[0] = row.getStringColumn("handle");
            entryData[1] = String.valueOf(row.getIntColumn("item_id"));
            entryData[2] = String.valueOf(row.getIntColumn("bitstream_id"));
            entryData[3] = row.getStringColumn("title");
            entryData[4] = row.getStringColumn("advisor");
            entryData[5] = row.getStringColumn("author");
            entryData[6] = row.getStringColumn("department");
            entryData[7] = row.getStringColumn("type");
            entryData[8] = row.getDateColumn("end_date").toString();
            writer.writeNext(entryData);
        }
    }

    @Override
    public void generate() throws IOException
    {
        log.info("CSV Writer generator for stats");
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
