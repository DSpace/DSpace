/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 *
 */
package edu.umd.lib.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.sql.SQLException;
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
import org.xml.sax.SAXException;

import au.com.bytecode.opencsv.CSVWriter;
import edu.umd.lib.dspace.content.EmbargoDTO;
import edu.umd.lib.dspace.content.factory.DrumServiceFactory;
import edu.umd.lib.dspace.content.service.EmbargoDTOService;

/**
 * Provides the Embargo List data in CSV form
 *
 * @author Peter Dietz (pdietz84@gmail.com)
 */
public class EmbargoListDownloader extends AbstractReader implements Recyclable
{
    protected static final Logger log = Logger
            .getLogger(EmbargoListDownloader.class);

    protected Response response;

    protected Request request;

    protected Context context;

    protected CSVWriter writer = null;
    
    protected EmbargoDTOService embargoDTOService = DrumServiceFactory.getInstance().getEmbargoDTOService();

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
        
        java.util.List<EmbargoDTO> embargoDTOs = embargoDTOService.getEmbargoList(context);

        writer.writeNext(new String[] { "Handle", "Item ID", "Bitstream ID",
                "Title", "Advisor", "Author", "Department", "Type", "End Date" });

        if (embargoDTOs.size() == 0)
        {
            return;
        }


        for (EmbargoDTO embargoETO : embargoDTOs)
        {
            String[] entryData = new String[9];

            entryData[0] = embargoETO.getHandle();
            entryData[1] = embargoETO.getItemIdString();
            entryData[2] = embargoETO.getBitstreamIdString();
            entryData[3] = embargoETO.getTitle();
            entryData[4] = embargoETO.getAdvisor();
            entryData[5] = embargoETO.getAuthor();
            entryData[6] = embargoETO.getDepartment();
            entryData[7] = embargoETO.getType();
            entryData[8] = embargoETO.getEndDateString();
            writer.writeNext(entryData);
        }
    }

    @Override
    public void generate() throws IOException
    {
        log.info("CSV Writer generator for Embargo List");
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
