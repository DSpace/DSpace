/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 * Helper class for Citation generator.
 * It creates de citation depending on the citation definition. 
 * 
 * This functionality is ported from the implementation done in 1.7.2 <damanzano>
 * 
 */

package org.dspace.app.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;


/**
 * Format the given field for the citation generation
 *
 * @author  Ying Jin
 * @version $Revision: ? $
 */

public class FormatIt
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(FormatIt.class);

    private static Pattern authorPattern1 = Pattern.compile(", \\d\\d\\d\\d");
    private static Pattern authorPattern2 = Pattern.compile("\\(\\d\\d\\d\\d");
    private static Pattern wrcPattern1 = Pattern.compile("Woodson Research Center");
    private static Pattern wrcPattern2 = Pattern.compile("MS \\d");


    /**
     * All format-it handled here
     *
     * @param formatit format-it setup
     * @param value
     * @return formatted value
     */
    public static String formatIt(String formatit, String value){
        if(formatit.equalsIgnoreCase("firstname-initial-only")){
            return getFirstNameInitials(value);
        }else if(formatit.equalsIgnoreCase("year-only")){
            return getYearOnly(value);
        }else if(formatit.equalsIgnoreCase("americas-author")){
            return getAmericasAuthor(value);
        }else if(formatit.equalsIgnoreCase("americas-ms-source")){
            return getAmericasMSSource(value);
        }else if(formatit.equalsIgnoreCase("ece-conferencedate")){
            return getECEConferenceDate(value);
        }else if(formatit.equalsIgnoreCase("get-wrc")){
            return getWRC(value);
        }else if(formatit.equalsIgnoreCase("icontec-author")){
            return getIcontecAuthor(value);
        }else if(formatit.equalsIgnoreCase("month-year")){
            return getMonthYear(value);
        }else if(formatit.equalsIgnoreCase("news-dates")){
            return getNewsDates(value);
        }else{    
            // do nothing
            return value;
        }

    }


    /**
     * Get the date in format of e.g. Oct. 2008
     *
     * @param date the date in YYYY-MM-DD format
     * @return formatted value
     */
    private static String getECEConferenceDate(String date){


        if(date.length() <=4) return date;

        StringBuffer dateBuffer = new StringBuffer();
        String[] dateValue = date.split("-");
        String[] toMonth = {"Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.", "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec."};
        String year = "";
        String mon = "";

        for(int i=0; i<dateValue.length; i++){
            if(i==0)
                year = dateValue[i];
            if(i==1)
                mon = toMonth[Integer.valueOf(dateValue[i]).intValue()-1];
        }
        if((mon !=null) && (mon != "")) dateBuffer.append(mon);
        if((year != null) && (year != "")) {
            dateBuffer.append(" ");
            dateBuffer.append(year);
        }
        return dateBuffer.toString();
    }

    /**
     * Get year only from date in YYYY-MM-DD format
     *
     * @param year
     * @return formatted value
     */
    private static String getYearOnly(String year){
        if(year.length() > 4){
            return year.substring(0,4);
        }
        return year;
    }

    /**
     * Get authors for amaricas project. This only requires to remove the date info
     *
     * @param author
     * @return formatted value
     */
    private static String getAmericasAuthor(String author){

        String parsedAuthor = author;

        Matcher authorMatcher1 = authorPattern1.matcher(author);
        Matcher authorMatcher2 = authorPattern2.matcher(author);

          if(authorMatcher1.find()){
            parsedAuthor = (authorPattern1.split(author))[0].trim();
        }else if(authorMatcher2.find()){
            parsedAuthor = (authorPattern2.split(author))[0].trim();
        }

        return parsedAuthor;
    }


    /**
     * Get part of the line before "MS \\d" from dc.source.collection 
     *
     * @param source
     * @return formatted value
     */
    private static String getAmericasMSSource(String source){
        String parsedSource="";
            String[] ss = source.split(",");

            for(int i=0; i<ss.length; i++){
                Matcher wrcMatcher2 = wrcPattern2.matcher(ss[i]);
                if(wrcMatcher2.find()){
                    parsedSource = ", " + parsedSource + ss[i];
                    return parsedSource + ".";
                }
                parsedSource += ss[i] + ", ";

            }
        return ".";
    }


    /**
     * Check and see if there are wrc in dc.source.collection
     *
     * @param source
     * @return "Woodson Research Center"
     */
    private static String getWRC(String source){

        Matcher wrcMatcher1 = wrcPattern1.matcher(source);
        if(wrcMatcher1.find()){
            return "Woodson Research Center";
        }else{
            return "";
        }
    }

    /**
     * get the first name initial only, e.g. F. N. Lastname
     *
     * @param name  full name
     * @return name with the firstname initial only
     */
    private static String getFirstNameInitials(String name){
        // seperate the firstname and lastname
        int cn = name.indexOf(",");
        if(cn != -1 ){
            String lastname = name.substring(0, cn);
            String firstname = name.substring(cn+1);

            String[] fns = firstname.trim().split(" ");
            String initialFirstname = "";

            for(int i=0; i<fns.length; i++){
              initialFirstname += fns[i].trim().substring(0, 1).toUpperCase();
              initialFirstname += ". ";
            }
            return (initialFirstname + lastname);

        }
        return name;
    }
    
    /**
     * Obtiene el apellido del autor en mayúsculas
     * 
     * @param name nombre completo del autor
     * @return nombre completo del autor con el apellido en mayúscula sostenida
     */

    private static String getIcontecAuthor(String name) {
        //separar el nombre del apellidio
        int cn=name.indexOf(",");
        if(cn!=-1){
            String lastname=name.substring(0,cn);
            String firstname=name.substring(cn);
            
            String fullname=lastname.toUpperCase()+firstname;
            return fullname;
        }
        return name;
    }
    
    /**
     * Obtiene la fecha en formato mes, año. e.g (Enero, 2011)
     * 
     * @param date La cadena de fehca guardada en el registro dublicore del item
     * @return La fecha pasada por parámetro expresada en formato mes, año. e.g (Enero, 2011).
     */
    private static String getMonthYear(String date) {
        if(date.length() <= 4){
            return date;
        }else{
            try{
                SimpleDateFormat df = null;
                // solo se tiene el més y el año
                if(date.length()<=7){
                    df=new SimpleDateFormat("yyyy-MM");
                }else{
                    //la fecha esta completa
                    df=new SimpleDateFormat("yyyy-MM-dd");
                }
                Date myDate=df.parse(date);
                SimpleDateFormat sdf=new SimpleDateFormat("MMMMM");
                String month=sdf.format(myDate);
                sdf=new SimpleDateFormat("yyyy");
                String year=sdf.format(myDate);
                String dateString=month+", "+year;
                return dateString;
            }catch(Exception e){
                log.info("Error en el formato de fecha "+e.getMessage());
            }
        }
        return date;
    }
    
    /**
     * Obtiene la fecha en formato dia, mes, año. e.g (01, Enero, 2011)
     * 
     * @param date La cadena de fehca guardada en el registro dublicore del item
     * @return La fecha pasada por parámetro expresada en formato dia, mes, año. e.g (01, Enero, 2011)
     */
    private static String getNewsDates(String date) {
        if(date.length() <= 4){
            return date;
        }else{
            try{
                SimpleDateFormat df = null;
                // solo se tiene el més y el año
                if(date.length()<=7){
                    df=new SimpleDateFormat("yyyy-MM");
                }else{
                    //la fecha esta completa
                    df=new SimpleDateFormat("yyyy-MM-dd");
                }
                
                Date myDate=df.parse(date);
                SimpleDateFormat sdf=new SimpleDateFormat("dd, MMMMM, yyyy");
                return sdf.format(myDate);                
            }catch(Exception e){
                log.info("Error en el formato de fecha "+e.getMessage());
            }
        }
        return date;
    }

}