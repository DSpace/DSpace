/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.StatisticsMetadataGenerator;

public class YearsAdditionalStatisticsData implements
        StatisticsMetadataGenerator
{
    private Calendar startFiscalYear = null;
    
    public YearsAdditionalStatisticsData()
    {
        startFiscalYear = Calendar.getInstance();
        startFiscalYear.set(Calendar.MONTH, Calendar.JULY);
        startFiscalYear.set(Calendar.DAY_OF_MONTH, 1);
    }
    
    @Override
    public void addMetadata(SolrInputDocument doc1, HttpServletRequest request,
            DSpaceObject dspaceObject)
    {
        Date date = null;
        try 
        {
            date = (Date) doc1.getFieldValue("time");
        }
        catch (ClassCastException e) {
            String dateString = (String) doc1.getFieldValue("time");
            SimpleDateFormat sdf = new SimpleDateFormat(SolrLogger.DATE_FORMAT_8601);
            try
            {
                date = sdf.parse(dateString);
            }
            catch (ParseException e1)
            {                
            }
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        
        int solarYear = cal.get(Calendar.YEAR);
        int fiscalYear = solarYear;
        if (cal.compareTo(startFiscalYear) < 0)
        {
            fiscalYear--;
        }
        doc1.addField("solarYear", solarYear);
        doc1.addField("fiscalYear", fiscalYear);
    }

}
