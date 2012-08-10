/*
 * DashboardViewer.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.xmlui.aspect.statisticsElasticSearch;

import org.apache.cocoon.environment.Request;
import org.apache.commons.validator.routines.DateValidator;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
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
 * @version
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
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

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
            return dateFormat.format(dateStart);
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
            return dateFormat.format(dateEnd);
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
     * {@inheritDoc}
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

    /**
     * Checks the parameters of the given request to see if they fit the
     * necessary criteria to run generate a report. The following must be true:
     *
     *  * from - Must be convertable to a valid date that is greater than the
     *    miniumum date and also less than or equal to the current date.
     *  * to - Must be convertable to a valid date that is greater than from
     *    and equal to or less than the current date.
     *
     * @return A map of valid parameters to their values.
     * @throws InvalidFormatException
     * @throws ParseException
     */
    private Map<String,String> checkAndNormalizeParameters(Map<String,String> params)  {
        try {

            //Create dateValidator and min and max dates
            DateValidator dateValidator = new DateValidator(false, DateFormat.SHORT);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

            Date maximumDate = new Date();
            Date minimumDate = dateFormat.parse(ReportGenerator.MINIMUM_DATE);

            //Check the to and from dates
            Date fromDate = null;
            Date toDate = null;
            boolean validToAndFrom = true;
            boolean hasFrom = params.containsKey("from") && params.get("from").length() > 0;
            boolean hasTo = params.containsKey("to") && params.get("to").length() > 0;

            if (hasFrom || hasTo) {
                if (hasFrom) {
                    fromDate = tryParse(params.get("from"));
                    params.put("from", dateFormat.format(fromDate));
                    validToAndFrom = validToAndFrom && dateValidator.compareDates(minimumDate, fromDate, null) <= 0;
                }
                if (hasTo) {
                    toDate = tryParse(params.get("to"));
                    params.put("to", dateFormat.format(toDate));
                    validToAndFrom = validToAndFrom && dateValidator.compareDates(toDate, maximumDate, null) <= 0;
                }
                if (hasFrom && hasTo) {
                    //Make sure hasFrom <= hasTo
                    validToAndFrom = validToAndFrom && dateValidator.compareDates(fromDate, toDate, null) <= 0;
                } else if (hasFrom && !hasTo) {
                    //Make sure hasFrom <= the max date
                    validToAndFrom = validToAndFrom && dateValidator.compareDates(fromDate, maximumDate, null) <= 0;
                } else {
                    //hasTo && !hasFrom
                    //Make sure hasTo >= the min date
                    validToAndFrom = validToAndFrom && dateValidator.compareDates(minimumDate, toDate, null) <= 0;
                }
                // Short circuit if the to and from dates are not valid
                if (!validToAndFrom) {
                    log.error("To and from dates are not within max/min or are not in order. "+ params.get("from") + " -> " + params.get("to"));
                    return null;
                }

                //Check fiscal
                if (params.containsKey("fiscal")) {
                    log.debug("fiscal: " + params.get("fiscal"));
                    if (Integer.parseInt(params.get("fiscal")) != 1) {
                        log.error("Fiscal field did not contain a proper value: " + params.get("fiscal"));
                    }
                }

            }
            return params;
        } catch (ParseException e) {
            log.error("ParseFormatException likely means a date format failed. "+e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }
}
