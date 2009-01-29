/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lims.dspace.app;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Set;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import java.text.SimpleDateFormat;

// SQL
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// IO
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
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
import org.dom4j.io.DocumentInputSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import org.xml.sax.InputSource;

// XSL
import javax.xml.transform.dom.DOMSource;  

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import javax.xml.transform.stream.StreamSource; 
import javax.xml.transform.stream.StreamResult; 

// XPath
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.FunctionContext;
import org.jaxen.Navigator;
import org.jaxen.SimpleFunctionContext;
import org.jaxen.XPathFunctionContext;

// Log4J
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// Lims
import edu.umd.lims.util.ErrorHandling;


/*********************************************************************
 Report on ETD entries in Proquest zip files.

 @author  Ben Wallberg

*********************************************************************/

public class EtdReport {

  private static Logger log = Logger.getLogger(EtdReport.class);

  static long lEtds = 0;

  static SAXReader reader = new SAXReader();

  static Map namespace = new HashMap();
  static Map mXPath = new HashMap();
  
  static DocumentFactory df = DocumentFactory.getInstance();


  /***************************************************************** main */
  /**
   * Command line interface.
   */

  public static void main(String args[]) throws Exception {


    ErrorHandling.setDefaultLogging();

    // Properties
    Properties props     = System.getProperties();

    // Loop through each zip file
    for (int x=0; x < args.length; x++) {
      String strZipFile = args[x];
      System.out.println("\nFile: " + strZipFile);

      ZipFile zip = new ZipFile(new File(strZipFile), ZipFile.OPEN_READ);

      // Get the list of entries
      Map map = readItems(zip);

      // Process each entry
      for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
        String strItem = (String)i.next();
        
        List item = (List)map.get(strItem);

        // Read the ETD metadata
        ZipEntry ze = (ZipEntry)item.get(1);
        Document meta = reader.read(new InputSource(zip.getInputStream(ze)));
        //System.out.println("ETD metadata:\n" + toString(meta));

        // Get the title
        String title = getXPath("/DISS_submission/DISS_description/DISS_title").selectSingleNode(meta).getText();
        
        System.out.println("  " + strItem + ": " + title);

        lEtds++;
        
      }
    }

    System.out.println("Items read: " + lEtds);
  }


  /********************************************************** readItems */
  /**
   * Read and compile the entries from the zip file.  Return a map; the
   * key is the item number, the value is list of file name/ZipEntry pairs 
   * with the first entry being the metadata and the second entry being the
   * primary pdf.
   */

  public static Map readItems(ZipFile zip) {

    Map map = new TreeMap();

    // Loop through the entries
    for (Enumeration e = zip.entries(); e.hasMoreElements(); ) {
      ZipEntry ze = (ZipEntry)e.nextElement();
      String strName = ze.getName();

      log.debug("zip entry: " + strName);

      // skip directories
      if (ze.isDirectory()) {
        continue;
      }

      String s[] = strName.split("/");

      if (s.length >= 2) {
        String strItem = s[0];
        String strFileName = s[s.length - 1];

        // Get the list
        ArrayList lmap = null;
        if (map.containsKey(strItem)) {
          lmap = (ArrayList)map.get(strItem);
        } else {
          lmap = new ArrayList();
          lmap.add(0, new Object());
          lmap.add(1, new Object());
          lmap.add(2, new Object());
          lmap.add(3, new Object());
          map.put(strItem, lmap);
        }

        // Put the file in the right position
        if (strFileName.equals("dissertation.xml") ||
            strFileName.equals("umi-umd-" + strItem + ".xml"))
          {
            lmap.set(0, strFileName);
            lmap.set(1, ze);
          }
        else if (strFileName.equals("dissertation.pdf") ||
                 strFileName.equals("umi-umd-" + strItem + ".pdf"))
          {
            lmap.set(2, strFileName);
            lmap.set(3, ze);
          }
        else {
          lmap.add(strFileName);
          lmap.add(ze);
        }

      }
    }

    return map;
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


  /************************************************************** toString */
  /**
   * Get string representation of xml Document.
   */

  public static String toString(Document doc) throws java.io.IOException {
    StringWriter sw = new StringWriter();
    OutputFormat format = OutputFormat.createPrettyPrint();
    XMLWriter writer = new XMLWriter(sw, format);
    writer.write(doc);
    return sw.toString();
  }

}



