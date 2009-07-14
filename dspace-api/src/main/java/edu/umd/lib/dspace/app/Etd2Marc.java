/*
 * Copyright (c) 2006 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lib.dspace.app;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Vector;
import java.util.HashSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

// IO
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

// XML
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.InvalidXPathException;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.Text;
import org.dom4j.XPath;

import org.dom4j.io.SAXReader;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.XMLWriter;

import org.xml.sax.InputSource;

// XSL
import javax.xml.transform.dom.DOMSource;  

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import javax.xml.transform.stream.StreamSource; 
import javax.xml.transform.stream.StreamResult; 

// XPath
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.FunctionContext;
import org.jaxen.Navigator;
import org.jaxen.SimpleFunctionContext;
import org.jaxen.XPathFunctionContext;

// Marc4J
import org.marc4j.MarcXmlReader;
import org.marc4j.MarcStreamWriter;

import org.marc4j.marc.Record;

// DSpace
import org.dspace.core.ConfigurationManager;


/*********************************************************************
 Convert ETD metadata into MARC for submission to TSD and loading 
 into Aleph.  Algorithm:

 <pre>
 open XSL transformation
 open output MARC file
 foreach directory on stdin
   get handle from 'mapfile'
   get files from 'import/item/contents'
   open <dissertation>.xml
   convert dissertation xml to marc xml using dom4j/xsl
   convert marc xml to marc using marc4j
   write marc to output file
 </pre>

 <pre>
 Revision History:

   2006/06/14: Ben
     - send the log file on the command line

   2006/03/23: Ben
     - initial version
 </pre>

 @author  Ben Wallberg

*********************************************************************/

public class Etd2Marc
{

  static long lRead = 0;
  static long lWritten = 0;

  static Transformer t = null;

  static Map namespace = new HashMap();
  static Map mXPath = new HashMap();

  static DocumentFactory df = DocumentFactory.getInstance();

  static Pattern pMeta = Pattern.compile("(dissertation|umi-umd-\\d*).xml");
  

  /***************************************************************** main */
  /**
   * Command line interface.
   */

  public static void main(String args[]) throws Exception
  {

    MarcStreamWriter marcwriter = null;
    PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"));

    try {

      // dspace dir
      String strDspace = ConfigurationManager.getProperty("dspace.dir");
      String strHandleProxy = ConfigurationManager.getProperty("handle.http-proxy");

      // the xml parser
      SAXReader reader = new SAXReader();

      // the transformer
      TransformerFactory tFactory = TransformerFactory.newInstance();
      t = tFactory.newTransformer(new StreamSource(new File(strDspace + "/load/etd2marc.xsl")));        

      // open the output file
      FileOutputStream fos = new FileOutputStream(new File(args[0]));
      marcwriter = new MarcStreamWriter(fos, "UTF-8");

      // Configure namespaces
      //handler.namespace.put("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
      //handler.namespace.put("sparql", "http://www.w3.org/2001/sw/DataAccess/rf1/result");
      //handler.namespace.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
      //handler.namespace.put("mets", "http://www.loc.gov/METS/");
      //handler.namespace.put("xlink", "http://www.w3.org/1999/xlink");

      // Read each directory from stdin
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      String strLine = null;
      while ((strLine = in.readLine()) != null) {

	out.println();
        out.println(strLine);
	out.flush();

        File dir = new File(strLine.trim());

	String strHandle = strHandleProxy + getHandle(dir);
	String strFiles = getFiles(dir);

        // Get the proquest xml file
        File files[] = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
              Matcher m = pMeta.matcher(name);
              return m.matches();
            }
          });
        
        if (files.length == 0) {
          out.println("  No proquest metadata file found");
          continue;
        } else if (files.length > 1) {
          out.println("  Too man proquest metadata files found");
          continue;
        }

        // Open the input
        InputSource is = new InputSource(new FileInputStream(files[0]));
	is.setEncoding("UTF-8");
	Document docProquest = reader.read(is);
        lRead++;
       
        // Display the title
        out.println("  " + getXPath("/DISS_submission/DISS_description//DISS_title").selectSingleNode(docProquest).getText());
        
        // Convert proquest format to marc xml
        DocumentSource source = new DocumentSource(docProquest);
        DocumentResult result = new DocumentResult();
        
        t.setParameter("files", strFiles);
        t.setParameter("handle", strHandle);
        t.transform(source, result);

	// Convert marc xml to marc
        StringWriter sw = new StringWriter();
        XMLWriter writer = new XMLWriter(sw);
        writer.write(result.getDocument());
        writer.flush();
	is = new InputSource(new StringReader(sw.toString()));
	MarcXmlReader convert = new MarcXmlReader(is);
	Record record = convert.next();

        // Write out the marc record
	marcwriter.write(record);

	lWritten++;
      }

    }
    finally {
      if (marcwriter != null) {
	marcwriter.close();
	out.close();
      }

      out.println();
      out.println("Records read:    " + lRead);
      out.println("Records written: " + lWritten);
    }
  }


  /************************************************************* getHandle */
  /**
   */

  public static String getHandle(File dir) throws IOException {
    File mapfile = new File(dir, "mapfile");
    String strHandle = "??";

    // Read through the file
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(mapfile)));

    String strLine = null;
    while ((strLine = br.readLine()) != null) {
      if (strLine.startsWith("item ")) {
	strHandle = strLine.substring(5);
      }
    }

    br.close();

    return strHandle;
  }


  /************************************************************** getFiles */
  /**
   */

  public static String getFiles(File dir) throws IOException {
    File file = new File(dir, "import/item/contents");

    HashSet h = new HashSet();

    // Read through the file
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

    String strLine = null;
    while ((strLine = br.readLine()) != null) {
      int n = strLine.lastIndexOf('.');
      if (n > -1) {
	h.add(strLine.substring(n+1).trim().toLowerCase());
      }
    }

    br.close();

    if (h.contains("mp3")) {
      return "Text and audio.";
    } else if (h.contains("jpg")) {
      return "Text and images.";
    } else if (h.contains("xls")) {
      return "Text and spreadsheet.";
    } else if (h.contains("wav")) {
      return "Text and video.";
    } else {
      return "Text.";
    }
  }


  /************************************************************* getXPath */
  /**
   * Get a compiled XPath object for the expression.  Cache.
   */

  private static XPath getXPath(String strXPath) throws InvalidXPathException {
    
    XPath xpath = null;

    if (mXPath.containsKey(strXPath)) {
        xpath = (XPath)mXPath.get(strXPath);

    } else {
        xpath = df.createXPath(strXPath);
        xpath.setNamespaceURIs(namespace);
        mXPath.put(strXPath, xpath);
    }

    return xpath;
  }

}



