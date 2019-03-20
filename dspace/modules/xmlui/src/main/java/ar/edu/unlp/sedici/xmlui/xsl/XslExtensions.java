package ar.edu.unlp.sedici.xmlui.xsl;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xpath.NodeSet;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Text;

import ar.edu.unlp.sedici.dspace.utils.Utils;
import ar.edu.unlp.sedici.util.SediciUtils;

/**
 * 
 * Clase estática que contiene las extensiones de xsl para permitir que
 * se usen desde las hojas xslt funciones java un poco mas complejas que
 * las provistas por xsl1.0, CUando se pase todo a xsl2, esta clase deberia
 * dejar de ser necesaria.
 * 
 * ver http://mukulgandhi.blogspot.com.ar/2009/11/xslt-10-regular-expression-string.html
 *
 */
public class XslExtensions {
	private static Logger log =  LoggerFactory.getLogger(XslExtensions.class);

	private XslExtensions() {
	}
	
	public static Text getBaseUrl(String str) throws ParserConfigurationException{
		URL uri;
        Text node;     
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        Document document = docBuilder.newDocument();        
        try {
			uri=new URL(str);
			node=document.createTextNode(uri.getHost());
		} catch (Exception e) {
			node=document.createTextNode("");
		}
		return node;
	}
	
	public static Boolean isUrl(String str) throws ParserConfigurationException{
		URL uri;    
        try {
			uri=new URL(str);
			return true;
		} catch (Exception e) {
			return false;
		}
		
	}

	public static NodeSet tokenize(String str, String regExp) throws ParserConfigurationException {
	      String[] tokens = str.split(regExp);
	      NodeSet nodeSet = new NodeSet();
	       
	      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	      DocumentBuilder docBuilder = dbf.newDocumentBuilder();
	      Document document = docBuilder.newDocument();
	       
	      for (int nodeCount = 0; nodeCount < tokens.length; nodeCount++) {
	        nodeSet.addElement(document.createTextNode(tokens[nodeCount]));   
	      }
	       
	      return nodeSet;
	    }
	
	public static String formatearFecha(String fecha, Locale locale){
		try{
			if (fecha.length()==7){
				SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM");
				Date fechaDate=sdf.parse(fecha);
				SimpleDateFormat sdf2=new SimpleDateFormat("MMMM yyyy", locale);			
				return sdf2.format(fechaDate);
				
			} else if (fecha.length()>=10) {
				//parseo el string que viene
				SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
				Date fechaDate=sdf.parse(fecha);
				
				DateFormat formatter=DateFormat.getDateInstance(1, locale);
				
				return formatter.format(fechaDate);
			}  else {
				return fecha;
			}
		} catch (ParseException e) {
			return fecha;
		}

	}
	
	public static String parseDateToYear(String date)
	{
		try{
			SimpleDateFormat sdf=new SimpleDateFormat("yyyy");
			Date parsedDate = sdf.parse(date);
			return sdf.format(parsedDate);
		}catch (ParseException e){
			return date;
		}
	}
	
	
	public static String codificarURL(String url){

		if (url == null){
			try{
				throw new NullPointerException();
			}catch (Exception e) {
				log.error("Se recibe null como url en XslExtension.codificarURL", e);
			}
			return "";
		}
		return SediciUtils.codificarURL(url);
	}
	
	public static boolean matches(String cadena, String regex) throws PatternSyntaxException{
		//handle/\d+/\d+/submit(.*)
		if (regex!=null && !regex.equals("")){
			if (cadena!=null && !cadena.equals("")){
				Pattern patron=Pattern.compile(regex);
				Matcher match=patron.matcher(cadena);		
				return match.matches();
			} else {
				return false;
			}
		} else {
			throw new PatternSyntaxException("El patrón de matcheo no está correctamente estructurado", regex, -1);
		}
	}

	//replace for a regular expresion
	public static String replace(String target, String regex, String replacement) {
		return target.replaceAll(regex, replacement);
	}
	
	//Encode an URL replacing some reserved chars
	public static String encodeURL(String urlToEncode) throws UnsupportedEncodingException{
		
		try {
			urlToEncode = URLEncoder.encode(urlToEncode, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return urlToEncode;
	}
	
	public static String getFileExtension(String filename) {
		return filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
	}

	public static int availableItemsCount() throws SQLException {
		return Utils.countAvailableItems(new Context());
	}
	
}
