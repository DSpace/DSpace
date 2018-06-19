/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statisticsElasticSearch;

import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Use a form to dynamically generate a variety of reports.
 *
 * @author "Ryan McGowan" ("mcgowan.98@osu.edu")
 * @author Peter Dietz (pdietz84@gmail.com)
 */
public class ReportGenerator
{
    /**
     * A logger for this class.
     */
    private static Logger log = Logger.getLogger(ReportGenerator.class);
    /**
     * The minimum date for the from or to field to be. (e.g. The beginning of DSpace)
     */
    private static String MINIMUM_DATE = "2008-01-01";
    private static ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("MM/dd/yyyy");
        }
    };

    // perfect input is 2008-01-22, an alternate format is 01/22/2008
    static String[] formatStrings = {"MM/dd/yyyy", "yyyy-MM-dd"};

    private Map<String, String> params;
    
    private Date dateStart;
    private Date dateEnd;

    public Date getDateStart() {
        return dateStart;
    }
    
    public String getDateStartFormated() {
        try {
            return dateFormat.get().format(dateStart);
        } catch (Exception e) {
            return "";
        }
    }
    
    public void setDateStart() {
        if(! params.containsKey("from")) {
            dateStart = null;
        } else {
            dateStart = tryParse(params.get("from"));
        }
    }
    
    public Date tryParse(String dateString) {
        if(dateString == null || dateString.length() == 0) {
            return null;
        }

        for(String formatString : formatStrings) {
            try {
                return new SimpleDateFormat(formatString).parse(dateString);
            } catch (ParseException e) {
                log.error("ReportGenerator couldn't parse date: " + dateString + ", with pattern of: "+formatString+" with error message:"+e.getMessage());
            }
        }
        return null;
    }

    public Date getDateEnd() {
        return dateEnd;
    }
    
    public String getDateEndFormatted() {
        try {
            return dateFormat.get().format(dateEnd);
        } catch (Exception e) {
            return "";
        }
    }
    
    public void setDateEnd() {
        if(! params.containsKey("to")) {
            dateEnd= null;
        } else {
            dateEnd = tryParse(params.get("to"));
        }
    }



    /**
     * Build the report generation form.
     *
     * @param parentDivision build it here.
     * @param request user request.
     * @see org.dspace.app.xmlui.cocoon.DSpaceTransformer#addBody(Body)
     */
    public void addReportGeneratorForm(Division parentDivision, Request request) {
        try {
            Division division = parentDivision.addDivision("report-generator", "primary");

            division.setHead("Report Generator");
            division.addPara("Used to generate reports with an arbitrary date range.");

            Division search = parentDivision.addInteractiveDivision("choose-report", request.getRequestURI(), Division.METHOD_GET, "primary");

            params = new HashMap<String, String>();
            for (Enumeration<String> paramNames = (Enumeration<String>) request.getParameterNames(); paramNames.hasMoreElements(); ) {
                String param = paramNames.nextElement();
                params.put(param, request.getParameter(param));
            }

            //params = checkAndNormalizeParameters(params);

            //Create Date Range part of form
            Para reportForm = search.addPara();

            setDateStart();
            Text from = reportForm.addText("from", "slick");
            from.setLabel("From");
            from.setHelp("The start date of the report, ex 01/31/2008");
            from.setValue(getDateStartFormated());

            setDateEnd();
            Text to = reportForm.addText("to", "slick");
            to.setLabel("To");
            to.setHelp("The end date of the report, ex 12/31/2012");
            to.setValue(getDateEndFormatted());

            //Add whether it is fiscal or not
            //CheckBox isFiscal = reportForm.addCheckBox("fiscal", "slick");
            //isFiscal.setLabel("Use Fiscal Years?");
            //Set up fiscal option with the correct default
            //isFiscal.addOption(params.containsKey("fiscal") && params.get("fiscal").equals("1"), 1, "");

            reportForm.addButton("submit_add").setValue("Generate Report");
        } catch (WingException e) {
            log.error(e.getMessage());
        }
    }

}
