package cz.cuni.mff.ufal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class PiwikHelper {


	public static String mergeXML(String report, String downloadReport) throws Exception {
		/**
		 * add page views from downloadReport as nb_downloads to report
		 */
		Document reportDoc = loadXMLFromString(report);
		Document downloadReportDoc = loadXMLFromString(downloadReport);
		
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		XPathExpression resExpr = xpath.compile("//result");
		XPathExpression downExpr = xpath.compile("./nb_downloads");
		XPathExpression uniqDownExpr = xpath.compile("./nb_uniq_downloads");
				
		NodeList rRows = (NodeList)resExpr.evaluate(reportDoc, XPathConstants.NODESET);

		for(int i=0;i<rRows.getLength();i++) {
			Node rRow = rRows.item(i);
			if(!rRow.hasChildNodes()) {
				Element nb_visits = reportDoc.createElement("nb_visits");
				nb_visits.setTextContent("0");
				Element nb_uniq_visitors = reportDoc.createElement("nb_uniq_visitors");
				nb_uniq_visitors.setTextContent("0");
				Element nb_pageviews = reportDoc.createElement("nb_pageviews");
				nb_pageviews.setTextContent("0");
				Element nb_uniq_pageviews = reportDoc.createElement("nb_uniq_pageviews");
				nb_uniq_pageviews.setTextContent("0");
				Element nb_downloads = reportDoc.createElement("nb_downloads");
				nb_downloads.setTextContent("0");
				Element nb_uniq_downloads = reportDoc.createElement("nb_uniq_downloads");
				nb_uniq_downloads.setTextContent("0");
				rRow.appendChild(nb_visits);
				rRow.appendChild(nb_uniq_visitors);
				rRow.appendChild(nb_pageviews);
				rRow.appendChild(nb_uniq_pageviews);
				rRow.appendChild(nb_downloads);
				rRow.appendChild(nb_uniq_downloads);
			}

			Node down = (Node) downExpr.evaluate(rRow, XPathConstants.NODE);
			Node uniqDown = (Node) uniqDownExpr.evaluate(rRow, XPathConstants.NODE);
			XPathExpression pvExpr = xpath.compile(String.format("/results/result[@%s]/nb_pageviews/text()", rRow.getAttributes().getNamedItem("date")));
			XPathExpression upvExpr = xpath.compile(String.format("/results/result[@%s]/nb_uniq_pageviews/text()", rRow.getAttributes().getNamedItem("date")));

			int pvCount = ((Double)pvExpr.evaluate(downloadReportDoc, XPathConstants.NUMBER)).intValue();
			int upvCount = ((Double)upvExpr.evaluate(downloadReportDoc, XPathConstants.NUMBER)).intValue();

			down.setTextContent(Integer.toString(pvCount));
			uniqDown.setTextContent(Integer.toString(upvCount));
		}

		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		Writer out = new StringWriter();
		tf.transform(new DOMSource(reportDoc), new StreamResult(out));
		return out.toString();
	}
	
	public static String mergeJSON(String report, String downloadReport) throws Exception {
		/**
		 * add page views from downloadReport as nb_downloads to report
		 */
		JSONParser parser = new JSONParser();
		JSONObject reportJSON = (JSONObject)parser.parse(report);
		JSONObject downloadReportJSON = (JSONObject)parser.parse(downloadReport);
		for(Object key : reportJSON.keySet()) {			
			JSONObject rRow = null;
			JSONObject dRow = null;
			try{
				dRow = (JSONObject)downloadReportJSON.get(key);
			} catch (ClassCastException e) {
				continue;
			}
			try {
				rRow = (JSONObject)reportJSON.get(key);
			} catch (ClassCastException e) {
				rRow = new JSONObject();
				reportJSON.put(key, rRow);
			}			
			rRow.put("nb_downloads", dRow.get("nb_pageviews"));
			rRow.put("nb_uniq_downloads", dRow.get("nb_uniq_pageviews"));
		}
		return reportJSON.toJSONString();
	}

	public static String mergeJSONResults(String report) throws Exception {
		JSONParser parser = new JSONParser();
		JSONArray json = (JSONArray)parser.parse(report);
		JSONObject views = (JSONObject)json.get(0);
		JSONObject downloads = (JSONObject)json.get(1);
		JSONObject result = new JSONObject();

		for(Object key : views.keySet()) {
			JSONObject view_data = new JSONObject();
			// any valid view data?
			try {
				view_data = (JSONObject) views.get(key);
			} catch (ClassCastException e) {
			}
			// any valid download data?
			try {
				JSONObject download_data = (JSONObject)downloads.get(key);
				view_data.put("nb_downloads", download_data.get("nb_pageviews"));
				view_data.put("nb_uniq_downloads", download_data.get("nb_uniq_pageviews"));
			} catch (ClassCastException e) {
			}
			result.put(key, view_data);
		}
		return result.toJSONString();
	}

	public static String readFromURL(String url) throws IOException {
		StringBuilder output = new StringBuilder();		
		URL widget = new URL(url);
        String old_value = "false";
        try{
            old_value = System.getProperty("jsse.enableSNIExtension");
            System.setProperty("jsse.enableSNIExtension", "false");

            BufferedReader in = new BufferedReader(new InputStreamReader(widget.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                output.append(inputLine).append("\n");
            }
            in.close();
        }finally {
        	//true is the default http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html
        	old_value = (old_value == null) ? "true" : old_value;
            System.setProperty("jsse.enableSNIExtension", old_value);
        }
		return output.toString();
	}

	public static Document loadXMLFromString(String xml) throws Exception {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(new StringReader(xml));
	    return builder.parse(is);
	}
		
}
