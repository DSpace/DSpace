package ar.edu.unlp.sedici.dspace.xmlui.util;

import java.util.Locale;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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
	
	public static String escapeURL(String url){
		if (url == null){
			try{
				throw new NullPointerException();
			}catch (Exception e) {
				log.error("escapeURL: Se recibe null como url", e);
			}
			return "";
		}
		char[] reservados={'!','#','$','%','&','(',')','*','+',',','/',':',';','=','?','@','[',']',']', ' '};
		for (char caracter: reservados) {
			url=url.replace(caracter, '_');
		}
		//remplazo la comilla simple
		url=url.replace("'", "_"); 	
 
		return url;
	}
	

	public static String getFileExtension(String filename) {
		return filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
	}
		

	public static String formatearFecha(String fecha,String idioma){
		String fechaParseada=fecha.split("T")[0];
		DateTime dt = new DateTime();		
		DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
		String mes=fmt.parseDateTime(fechaParseada).monthOfYear().getAsText();
		String fechaFinal=fechaParseada.split("-")[2]+"-"+mes+"-"+fechaParseada.split("-")[0];
		DateTimeFormatter fmt2 = DateTimeFormat.forPattern("dd-MMMM-yyyy");
		String resul;
		switch (idioma){
		case "en":
				resul= fmt2.parseDateTime(fechaFinal).toString("dd-MMMM-yyyy",Locale.US);
				resul=resul.replace("-", " of ");
				break;
		
		default:
			resul= fmt2.parseDateTime(fechaFinal).toString("dd-MMMM-yyyy",Locale.getDefault());
			resul=resul.replace("-", " de ");
			break;
		}
		return resul;


	}
}

