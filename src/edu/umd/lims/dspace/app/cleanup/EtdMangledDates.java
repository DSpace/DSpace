/*
 * Copyright (c) 2007 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lims.dspace.app.cleanup;

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

import java.text.SimpleDateFormat;

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

import org.dspace.browse.Browse;

// Lims
import edu.umd.lims.util.ErrorHandling;


/*********************************************************************
 Cleanup mangled dates caused, during ETD loading, by Proquest changing
 the date format.

 @author  Ben Wallberg

*********************************************************************/

public class EtdMangledDates
{

  private static Logger log = Logger.getLogger(EtdMangledDates.class);

  static long lRead = 0;
  static long lWritten = 0;

  static SAXReader reader = new SAXReader();
  static Transformer tDC = null;

  static Map namespace = new HashMap();
  static Map mXPath = new HashMap();
  
  static DocumentFactory df = DocumentFactory.getInstance();

  static Collection etdcollection = null;
  static EPerson etdeperson = null;

  static SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");


  /***************************************************************** main */
  /**
   * Command line interface.
   */

  public static void main(String args[]) throws Exception
  {

    Context context = new Context();
    context.setIgnoreAuthorization(true);

    try {

      // Properties
      Properties props     = System.getProperties();
      String strInFile     = props.getProperty("infile", null);
      String strInDir      = props.getProperty("indir", null);

      // dspace dir
      String strDspace     = ConfigurationManager.getProperty("dspace.dir");

      // logging (log4j.defaultInitOverride needs to be set or
      // config/log4j.properties will be read and used additionally)
      PropertyConfigurator.configure(strDspace + "/config/log4j-app.properties");

      // the transformers
      TransformerFactory tFactory = TransformerFactory.newInstance();
      tDC = tFactory.newTransformer(new StreamSource(new File(strDspace + "/load/etd2dc.xsl")));        

      // Open the infile
      BufferedReader infile = new BufferedReader(new InputStreamReader(new FileInputStream(strInFile)));

      // Process each infile entry
      String strLine = null;
      while ((strLine = infile.readLine()) != null) {
	StringTokenizer st = new StringTokenizer(strLine, "\t");
	String strItem = st.nextToken();
	String strHandle= st.nextToken();

	log.info("\nItem: " + strItem + ", "
		 + "Handle: " + strHandle);

	strHandle = strHandle.substring(22);

	// get the metadata
	String strMeta = 
	  strInDir + "/" + strItem + "/"
	  + "umi-umd-" + strItem + ".xml"
	  ;
	FileInputStream isMeta = new FileInputStream(strMeta);

	Document meta = reader.read(new InputSource(isMeta));

	// get the issue date
	DocumentSource source = new DocumentSource(meta);
	DocumentResult result = new DocumentResult();

	tDC.transform(source, result);

	Document dc = result.getDocument();

	String strNewDate = getXPath("/dublin_core/dcvalue[@element='date' and @qualifier='issued']").selectSingleNode(dc).getText();
	  
	log.info("  issue date from meta: " + strNewDate);

	// get the item
	Item item = (Item)HandleManager.resolveToObject(context, strHandle);

	DCValue dcval[] = item.getDC("title", null, Item.ANY);
	log.info("  title: " + dcval[0].value);

	lRead++;

	// get the existing issue date
	dcval = item.getDC("date", "issued", Item.ANY);
	String strOldDate = dcval[0].value;
	log.info("  issue date in db: " + strOldDate);

	if (! strNewDate.equals(strOldDate)) {
	  // update needed
	  item.clearDC("date", "issued", Item.ANY);
	  item.addDC("date", "issued", null, new String[] {strNewDate});

	  item.update();

	  lWritten++;
	}
      }

      context.commit();
    }

    catch (Exception e) {
      log.error("Uncaught exception: " + ErrorHandling.getStackTrace(e));
      System.exit(1);
    }

    finally {
      if (context != null) {
	try { context.complete(); } catch (Exception e) {}
      }

      log.info("=====================================\n" +
	       "Records read:    " + lRead + "\n" +
	       "Records written: " + lWritten + "\n");

    }

    System.exit(0);
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


  /*********************************************************** reportItem */
  /**
   * Report a successfully loaded item
   */

  private static void reportItem(Context c, Item item, String strHandle, Set sCollections) throws Exception {

    StringBuffer sb = new StringBuffer();

    sb.append("Item loaded: " + strHandle + "\n");

    // Title
    DCValue dc[] = item.getDC("title", null, Item.ANY);
    sb.append("  Title: " + dc[0].value + "\n");

    // Collections
    sb.append("  Collection: " + etdcollection.getMetadata("name") + "\n");
    for (Iterator ic = sCollections.iterator(); ic.hasNext(); ) {
      Collection coll = (Collection)ic.next();
      sb.append("  Collection: " + coll.getMetadata("name") + "\n");
    }

    log.info(sb.toString());
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



