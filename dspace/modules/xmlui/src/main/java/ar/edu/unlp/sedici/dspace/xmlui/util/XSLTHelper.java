package ar.edu.unlp.sedici.dspace.xmlui.util;

import java.util.Locale;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.dspace.app.util.Util;

public class XSLTHelper {
	
	private final static Logger log = Logger.getLogger(XSLTHelper.class);
	
	public static String replaceAll(String source, String regex, String replacement) {
		return source.replaceAll(regex, replacement);
	}
	
	public static String getLugarDesarrollo(String source) {
 		int first = source.indexOf("(");
 		int last = source.indexOf(")");
 		if (first != -1 && last != -1) {
 			return source.substring(first + 1, last);
 		} else {
 			return source;
 		}
 	}
	
	public static String stripDash(String source) {
		if (source.endsWith("-")) {
			return source.substring(0, source.length() - 1);
		} else {
			return source;
		}
	}
	
	public static String escapeURL(String url) throws NullPointerException{
		if (url == null){
			try{
				throw new NullPointerException();
			}catch (Exception e) {
				log.error("escapeURL: Se recibe null como url", e);
			}
		}else{
			try{
				return Util.encodeBitstreamName(url);
			}catch (Exception e){
				log.error("Cannot escape the url specified: " + url);
			}
		}
		return "";
	}
	

	public static String getFileExtension(String filename) {
		return filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
	}
		

	public static String formatearFecha(String date,String language){
		//Si la fecha viene con el formato aaaa-mm-ddTminutos:segundos:milesimasZ me quedo solo con la fecha
		 String parsedDate=date.split("T")[0];
		 Locale locale=null;
		 String toReplace="";
		 DateTime dt = new DateTime();
		 DateTimeFormatter fmt; 
		 String resul,finalDate;
		 String day="";
		 String month="";
		 String[] formats = {"yyyy-MM-dd","yyyy-MM","yyyy"};
		 String[] finalFormats = {"dd-MMMM-yyyy","MMMM-yyyy","yyyy"};
		 for(int i=0;i<formats.length;i++)
		 {
			 try
			 {
				 fmt = DateTimeFormat.forPattern(formats[i]);
				 fmt.parseDateTime(parsedDate);
				 if(formats[i]!="yyyy")
				 {
					 month=fmt.parseDateTime(parsedDate).monthOfYear().getAsText()+"-";
				 }
				 if(formats[i]=="yyyy-MM-dd")
				 {
					 day=fmt.parseDateTime(parsedDate).getDayOfMonth()+"-";
				 }				 
				 finalDate=day+month+parsedDate.split("-")[0];
			  	 fmt = DateTimeFormat.forPattern(finalFormats[i]);			  
			 	 switch (language)
			 	 {
				 case "en":
					 	locale=Locale.US;
						toReplace=" of ";
						break;
				
		 		 default:
		 			locale=Locale.getDefault();
					toReplace=" de ";
					break;
				 }
			 	resul= fmt.parseDateTime(finalDate).toString(finalFormats[i],locale);
				resul=resul.replace("-",toReplace);
			 	return resul;
				 
			 }
			 catch (java.lang.IllegalArgumentException e)
			 {
				
			 }
		 }
		 return "";
		

	}
}

