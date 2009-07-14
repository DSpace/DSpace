/*
 * Copyright (c) 2007 The University of Maryland. All Rights Reserved.
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
import java.util.StringTokenizer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.zip.GZIPInputStream;

import java.util.regex.Pattern;

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
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import org.xml.sax.InputSource;

import org.xml.sax.helpers.AttributesImpl;

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

// DSpace
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Email;

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;

import org.dspace.handle.HandleManager;


// Marc4J
import org.marc4j.MarcXmlReader;
import org.marc4j.MarcStreamWriter;

import org.marc4j.marc.Record;

// Lims
import edu.umd.lims.util.ErrorHandling;


/*********************************************************************
 ISR Loader. Algorithm:

 <pre>
   get params
     input file
     input pdf directory

   get properties

   open input file
   foreach item
     extract dublin core (already created)
     load item
 </pre>

 @author  Ben Wallberg

*********************************************************************/

public class CissmLoader implements ElementHandler {

  private static Logger log = Logger.getLogger(CissmLoader.class);

  static long lRead = 0;
  static long lWritten = 0;

  static SAXReader reader = new SAXReader();

  static Map namespace = new HashMap();
  static Map mXPath = new HashMap();
  
  static DocumentFactory df = DocumentFactory.getInstance();

  static Collection cissmcollection = null;
  static EPerson cissmeperson = null;

  static String strBitstreamDir = null;
  static boolean bLoad = false;


  /***************************************************************** main */
  /**
   * Command line interface.
   */

  public static void main(String args[]) throws Exception {

    try {

      // Properties
      Properties props     = System.getProperties();
      String strInFile     = props.getProperty("cissmloader.infile", null);
      String strLoad       = props.getProperty("cissmloader.load", "false");
      strBitstreamDir      = props.getProperty("cissmloader.bitstreamdir", null);

      bLoad = strLoad.equals("true");

      // dspace dir
      String strDspace     = ConfigurationManager.getProperty("dspace.dir");
      String strEPerson    = ConfigurationManager.getProperty("cissmloader.eperson");
      String strCollection = ConfigurationManager.getProperty("cissmloader.collection");

      // logging (log4j.defaultInitOverride needs to be set or
      // config/log4j.properties will be read and used additionally)
      PropertyConfigurator.configure(strDspace + "/config/log4j-app.properties");

      // Get DSpace values
      Context context = new Context();

      if (strCollection == null) {
	throw new Exception("isrloader.collection not set");
      }
      cissmcollection = Collection.find(context, Integer.parseInt(strCollection));
      if (cissmcollection == null) {
	throw new Exception("Unable to find cissmloader.collection: " + strCollection);
      }

      if (strEPerson == null) {
	throw new Exception("cissmloader.eperson not set");
      }
      cissmeperson = EPerson.findByEmail(context, strEPerson);
      if (cissmeperson == null) {
	throw new Exception("Unable to find cissmloader.eperson: " + strEPerson);
      }

      context.complete();

      // Open the input file
      InputStream is = new FileInputStream(strInFile);

      // Read through the input records
      CissmLoader handler = new CissmLoader();
      reader.addHandler("/collection/record", handler);
      reader.read(is);

    }

    catch (Exception e) {
      log.error("Uncaught exception: " + ErrorHandling.getStackTrace(e));
      System.exit(1);
    }

    finally {
      log.info("=====================================\n" +
	       "Records read:    " + lRead + "\n" +
	       "Items created:   " + lWritten + "\n"
	       );
    }

    System.exit(0);
  }


  /*************************************************************** onStart */
  /**
   * Start of element handler.
   */

  public void onStart(ElementPath path) {
    // do nothing here...    
  }


  /**************************************************************** onEnd */
  /**
   * End of element handler.
   */

  public void onEnd(ElementPath path) {
    try {
      // get the subtree
      Element record = path.getCurrent();

      // turn into standalone Document
      record.detach();
      Document doc = df.createDocument(record);

      lRead++;

      String strTitle = getXPath("/record/dublin_core/dcvalue[@element='title']").selectSingleNode(doc).getText();
      String strFileName = getXPath("/record/fileName").selectSingleNode(doc).getText();
      log.info("\nTitle: " + strTitle);
      log.info("  fileName: " + strFileName);

      // Extract the dublin core
      Element e = (Element)getXPath("/record/dublin_core").selectSingleNode(doc);
      e.detach();
      Document dc = df.createDocument(e);

      // Load the item
      if (bLoad) {
	loadItem(doc, dc, strFileName);
      }

    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }


  /************************************************************* loadItem */
  /**
   * Load one item into DSpace.
   */

  public static void loadItem(Document meta, Document dc, String strFileName) {
    Context context = null;

    try {
      // Setup the context
      context = new Context();
      context.setCurrentUser(cissmeperson);
      context.setIgnoreAuthorization(true);

      // Create a new Item, started in a workspace
      WorkspaceItem wi = WorkspaceItem.create(context, cissmcollection, false);
      Item item = wi.getItem();

      // Add dublin core
      addDC(context, item, dc);

      // Add bitstreams
      if (addBitstreams(context, item, strFileName)) {

	// Finish installation into the database
	InstallItem.installItem(context, wi);

	// Get the handle
	String strHandle = HandleManager.findHandle(context, item);
	strHandle = HandleManager.getCanonicalForm(strHandle);
	
	context.commit();

	lWritten++;

	// Report the created item
	log.info("  written: " + strHandle);
      }
      else {
	context.abort();
      }
    }

    catch (Exception e) {
      log.error("Error loading item: " +
		ErrorHandling.getStackTrace(e));
      if (context != null) {
	context.abort();
      }
    }

    finally {
      if (context != null) {
	try { context.complete(); } catch (Exception e) {}
      }
    }
  }

  /********************************************************* addBitstreams */
  /**
   * Add bitstreams to the item.
   */

  public static boolean addBitstreams(Context context, Item item, String strFileName) throws Exception {

    // Get the ORIGINAL bundle which contains public bitstreams
    Bundle[] bundles = item.getBundles("ORIGINAL");
    Bundle bundle = null;

    if (bundles.length < 1) {
	bundle = item.createBundle("ORIGINAL");
    } else {
	bundle = bundles[0];
    }

    // Build the file name
    String strDiskName = strBitstreamDir + "/" + strFileName;
    File f = new File(strDiskName);

    if (! f.exists() || f.length() == 0) {
      log.info("  unable to load " + strFileName);
      return false;
    }

    // Create the bitstream
    InputStream is = new FileInputStream(f);
    Bitstream bs = bundle.createBitstream(is);
    bs.setName(strFileName);
    
    // Set the format
    BitstreamFormat bf = FormatIdentifier.guessFormat(context, bs);
    bs.setFormat(bf);
    
    bs.update();

    return true;
  }


  /***************************************************************** addDC */
  /**
   * Add dublin core to the item
   */

  public static void addDC(Context context, Item item, Document dc) throws Exception {

    // Loop through the elements
    List l = getXPath("/dublin_core/dcvalue").selectNodes(dc);
    for (Iterator i = l.iterator(); i.hasNext(); ) {
      Node ndc = (Node)i.next();

      String value = ndc.getText();

      String element = getXPath("@element").selectSingleNode(ndc).getText();

      Node n = getXPath("@qualifier").selectSingleNode(ndc);
      String qualifier = ((n == null || n.getText().equals("none")) ? null : n.getText());
	
      n = getXPath("@language").selectSingleNode(ndc);
      String language = ((n == null || n.getText().equals("none"))? null : n.getText());
      if (language == null) {
	language = ConfigurationManager.getProperty("default.language");
      }

      item.addDC(element, qualifier, language, value);
      log.debug(element + ":" + qualifier + ":" + language + ":" + value);
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
