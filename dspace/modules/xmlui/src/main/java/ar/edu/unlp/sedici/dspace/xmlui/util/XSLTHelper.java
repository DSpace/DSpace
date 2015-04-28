package ar.edu.unlp.sedici.dspace.xmlui.util;

import org.apache.log4j.Logger;

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
}

