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
		

	public static String formatearFecha(String fecha,String language){
		 String fechaParseada=fecha.split("T")[0];
		 DateTime dt = new DateTime();
		 DateTimeFormatter fmt,fmt2; 
		 String resul,finalDate;
		 String day="";
		 String month="";
		 String[] formats = {"yyyy-MM-dd","yyyy-MM","yyyy"};
		 String[] secondFormats = {"dd-MMMM-yyyy","MMMM-yyyy","yyyy"};
		 for(int i=0;i<formats.length;i++){
			 try{
				 fmt = DateTimeFormat.forPattern(formats[i]);
				 fmt.parseDateTime(fechaParseada);
				 if(formats[i]!="yyyy")
				 {
					 month=fmt.parseDateTime(fechaParseada).monthOfYear().getAsText()+"-";
				 }
				 if(formats[i]=="yyyy-MM-dd")
				 {
					 day=fmt.parseDateTime(fechaParseada).getDayOfMonth()+"-";
				 }				 
				 finalDate=day+month+fechaParseada.split("-")[0];
			  	 fmt2 = DateTimeFormat.forPattern(secondFormats[i]);
			 	 resul= fmt2.parseDateTime(finalDate).toString(secondFormats[i],Locale.getDefault());
			 	 switch (language){
				 case "en":
						resul= fmt2.parseDateTime(finalDate).toString(secondFormats[i],Locale.US);
						resul=resul.replace("-", " of ");
						break;
				
		 		 default:
					resul= fmt2.parseDateTime(finalDate).toString(secondFormats[i],Locale.getDefault());
					resul=resul.replace("-", " de ");
					break;
				 }
			 	 return resul;
				 
			 }
			 catch (java.lang.IllegalArgumentException e)
			 {
				
			 }
		 }
		 return "";
		

	}
}

