package ar.edu.unlp.sedici.xmlui.xsl;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xpath.NodeSet;
import org.w3c.dom.Document;

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

	private XslExtensions() {
	}
	
	public static NodeSet isURL(String str) throws ParserConfigurationException {
		//TODO
		return null;
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
}
