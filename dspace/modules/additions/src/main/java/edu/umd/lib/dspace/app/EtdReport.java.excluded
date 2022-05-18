/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lib.dspace.app;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// Log4J
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.InvalidXPathException;
import org.dom4j.XPath;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.InputSource;

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
    
  private final static ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();


  /***************************************************************** main */
  /**
   * Command line interface.
   */

  public static void main(String args[]) throws Exception {


    ErrorHandling.setDefaultLogging();

    String strDspace     = configurationService.getProperty("dspace.dir");

    // Log4j configuration
    PropertyConfigurator.configure(strDspace + "/config/log4j-app.properties");

    // Properties
    Properties props     = System.getProperties();

    // Loop through each zip file
    for (int x=0; x < args.length; x++) {
      String strZipFile = args[x];
      System.out.println("\nFile: " + strZipFile);

      ZipFile zip = new ZipFile(new File(strZipFile), ZipFile.OPEN_READ);

      // Get the list of entries
      Map map = EtdLoader.readItems(zip);

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

        // List the files
        for (Iterator j = item.iterator(); j.hasNext(); ) {
          System.out.println("    " + j.next());
          j.next();
        }

        lEtds++;
        
      }
    }

    System.out.println("Items read: " + lEtds);
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



