/*
 * Copyright (c) 2007 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lib.dspace.app.cleanup;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import java.text.SimpleDateFormat;

// IO
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;

// XML
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.InvalidXPathException;
import org.dom4j.XPath;

import org.dom4j.io.SAXReader;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import org.xml.sax.InputSource;

// XSL

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import javax.xml.transform.stream.StreamSource;

// Log4J
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
// DSpace
import org.dspace.core.Context;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

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

  private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

  protected final static HandleService handleService = HandleServiceFactory.getInstance().getHandleService();;

  protected final static ItemService itemService = ContentServiceFactory.getInstance().getItemService();

  /***************************************************************** main */
  /**
   * Command line interface.
   */

  public static void main(String args[]) throws Exception
  {

    Context context = new Context();
    context.turnOffAuthorisationSystem();

    try {

      // Properties
      Properties props     = System.getProperties();
      String strInFile     = props.getProperty("infile", null);
      String strInDir      = props.getProperty("indir", null);

      // dspace dir
      String strDspace     = configurationService.getProperty("dspace.dir");

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
        Item item = (Item)handleService.resolveToObject(context, strHandle);

        String title = itemService.getMetadataFirstValue(item, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
        log.info("  title: " + title);

        lRead++;

        // get the existing issue date
        
        String strOldDate = itemService.getMetadataFirstValue(item, MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY);
        log.info("  issue date in db: " + strOldDate);

        if (! strNewDate.equals(strOldDate)) {
          // update needed
          itemService.clearMetadata(context,item, MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY);
          itemService.addMetadata(context, item, MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY, Arrays.asList(strNewDate));
          itemService.update(context, item);

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
    String title = itemService.getMetadataFirstValue(item, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
	  sb.append("  Title: " + title + "\n");

    // Collections
    sb.append("  Collection: " + etdcollection.getName() + "\n");
    for (Iterator ic = sCollections.iterator(); ic.hasNext(); ) {
      Collection coll = (Collection)ic.next();
      sb.append("  Collection: " + coll.getName() + "\n");
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



