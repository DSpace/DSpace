/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.utils;

import org.apache.commons.lang.StringUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author Will Simpson
 * 
 */
public class DateUtils {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d+)(?:-(\\d+))?(?:-(\\d+))?(T\\d\\d:\\d\\d:\\d\\d)?");

    //Thread safe: see source http://www.docjar.com/html/api/com/sun/org/apache/xerces/internal/jaxp/datatype/DatatypeFactoryImpl.java.html
    //see also analysis: http://www.javajirawat.com/2015/09/xmlgregoriancalendar-datatypefactory.html
    private static DatatypeFactory dataTypeFactory;
    
    /**
     * @see http 
     *      ://www.crossref.org/schema/info/CrossRefSchemaDocumentation4.1.0.pdf
     */
    private static Map<String, String> seasonsAndQuartersToMonth = new HashMap<String, String>();
    static {
        seasonsAndQuartersToMonth.put("21", "03");
        seasonsAndQuartersToMonth.put("22", "06");
        seasonsAndQuartersToMonth.put("23", "09");
        seasonsAndQuartersToMonth.put("24", "12");
        seasonsAndQuartersToMonth.put("31", "01");
        seasonsAndQuartersToMonth.put("32", "04");
        seasonsAndQuartersToMonth.put("33", "07");
        seasonsAndQuartersToMonth.put("34", "10");
    }

    public static XMLGregorianCalendar convertToXMLGregorianCalendar(String dateString) {
        return convertToXMLGregorianCalendar(dateString, true);
    }
    
    public static XMLGregorianCalendar convertToXMLGregorianCalendar(String dateString, boolean tidy) {
        String tidyDateString = dateString;
        if(tidy) {
            tidyDateString = tidy(dateString);
        }
        if (tidyDateString == null) {
            return null;
        }
        DatatypeFactory dataTypeFactory = createDataTypeFactory();
        try {
            return dataTypeFactory.newXMLGregorianCalendar(tidyDateString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static XMLGregorianCalendar convertToXMLGregorianCalendar(long time) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(time);
        return convertToXMLGregorianCalendar(gregorianCalendar);
    }
    
    public static XMLGregorianCalendar convertToXMLGregorianCalendar(GregorianCalendar gregorianCalendar) {
        return createDataTypeFactory().newXMLGregorianCalendar(gregorianCalendar);
    }

    public static XMLGregorianCalendar convertToXMLGregorianCalendar(Date date) {
        if (date == null) {
            return null;
        }
        return convertToXMLGregorianCalendar(date.getTime());
    }

    public static XMLGregorianCalendar convertToXMLGregorianCalendarNoTimeZoneNoMillis(Date date) {
        XMLGregorianCalendar basicCalender = convertToXMLGregorianCalendar(date);
        basicCalender.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
        basicCalender.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
        return basicCalender;
    }

    public static Date convertToDate(String dateString) {
        String tidyDateString = tidy(dateString);
        if (tidyDateString == null) {
            return null;
        }
        return convertToXMLGregorianCalendar(tidyDateString).toGregorianCalendar().getTime();
    }

    public static Date convertToDate(XMLGregorianCalendar xmlGregorianCalendar) {
        if (xmlGregorianCalendar == null) {
            return null;
        }
        return xmlGregorianCalendar.toGregorianCalendar().getTime();
    }

    private static String tidy(String dateString) {
        if (dateString == null) {
            return null;
        }
        Matcher matcher = DATE_PATTERN.matcher(dateString);
        if (!matcher.matches()) {
            return null;
        }
        String year = matcher.group(1);
        String month = matcher.group(2);
        String day = matcher.group(3);
        String time = matcher.group(4);

        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.leftPad(year, 4, '0'));
        if (month != null) {
            builder.append('-');
            month = mapSeasonsAndQuartersToMonth(month);
            builder.append(StringUtils.leftPad(month, 2, '0'));

        }
        if (day != null) {
            builder.append('-');
            builder.append(StringUtils.leftPad(day, 2, '0'));
        }
        if (time != null) {
            builder.append(time);
        }
        return builder.toString();
    }

    public static boolean olderThan(Date date, int days) {
        return date.getTime() < new Date().getTime() - days * 24 * 60 * 60 * 1000;
    }

    private static String mapSeasonsAndQuartersToMonth(String month) {
        if (seasonsAndQuartersToMonth.containsKey(month)) {
            return seasonsAndQuartersToMonth.get(month);
        } else {
            return month;
        }
    }

    private static DatatypeFactory createDataTypeFactory() {
        if (dataTypeFactory == null){
            synchronized (DateUtils.class){
                if (dataTypeFactory == null)
                    try {
                        dataTypeFactory = DatatypeFactory.newInstance();
                    } catch (DatatypeConfigurationException e) {
                        throw new RuntimeException("Couldn't create org.orcid.test.data type factory", e);
                    }
            }
        }
        return dataTypeFactory;
    }

}
