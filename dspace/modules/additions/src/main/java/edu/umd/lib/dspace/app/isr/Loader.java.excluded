/*
 * Copyright (c) 2007 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lib.dspace.app.isr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

// Log4J
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.InvalidXPathException;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
// DSpace
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.UUIDUtils;
import org.xml.sax.helpers.AttributesImpl;

// Lims
import edu.umd.lims.util.ErrorHandling;


/*********************************************************************
 ISR Loader. Algorithm:

 <pre>
   get params
     input file
     input pdf directory
     output dublin core

   get properties

   open input file
   foreach item
     metadata fixups for input item
     xsl metadata to dublin core
     output dublin core
     load item
 </pre>

 @author  Ben Wallberg

*********************************************************************/

public class Loader  implements ElementHandler {

  private static Logger log = Logger.getLogger(Loader.class);

  static long lRead = 0;
  static long lWritten = 0;

  static SAXReader reader = new SAXReader();
  static Transformer tDC = null;

  static Map namespace = new HashMap();
  static Map mXPath = new HashMap();
  
  static DocumentFactory df = DocumentFactory.getInstance();

  static Collection isrcollection = null;
  static EPerson isreperson = null;

  static XMLWriter out = null;
  static boolean bOutFile = false;
  
  static String strPdfDir = null;
  static boolean bLoad = false;

  private final static ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
  
  private final static HandleService handleService = HandleServiceFactory.getInstance().getHandleService();;

  private final static CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
  
  private final static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
  
  private final static BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
  
  private final static BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

  private final static BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();

  private final static WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

  private final static InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();

  private final static EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();



  /***************************************************************** main */
  /**
   * Command line interface.
   */

  public static void main(String args[]) throws Exception {

    try {

      // Properties
      Properties props     = System.getProperties();
      String strInFile     = props.getProperty("isrloader.infile", null);
      String strOutFile    = props.getProperty("isrloader.outfile", null);
      String strLoad       = props.getProperty("isrloader.load", "false");
      strPdfDir            =  props.getProperty("isrloader.pdfdir", null);

      bOutFile = (strOutFile != null);
      bLoad = strLoad.equals("true");

      // dspace dir
      String strDspace     = configurationService.getProperty("dspace.dir");
      String strEPerson    = configurationService.getProperty("isrloader.eperson");
      String strCollection = configurationService.getProperty("isrloader.collection");

      // logging (log4j.defaultInitOverride needs to be set or
      // config/log4j.properties will be read and used additionally)
      PropertyConfigurator.configure(strDspace + "/config/log4j-app.properties");

      // the transformers
      TransformerFactory tFactory = TransformerFactory.newInstance();
      tDC = tFactory.newTransformer(new StreamSource(new File(strDspace + "/load/isr2dc.xsl")));        

      // Get DSpace values
      Context context = new Context();

      if (strCollection == null) {
	      throw new Exception("isrloader.collection not set");
      }
      isrcollection = collectionService.find(context, UUIDUtils.fromString(strCollection));
      if (isrcollection == null) {
	      throw new Exception("Unable to find isrloader.collection: " + strCollection);
      }

      if (strEPerson == null) {
      	throw new Exception("isrloader.eperson not set");
      }
      isreperson = epersonService.findByEmail(context, strEPerson);
      if (isreperson == null) {
	      throw new Exception("Unable to find isrloader.eperson: " + strEPerson);
      }

      context.complete();

      // Open the input file
      InputStream is = new FileInputStream(strInFile);

      if (bOutFile) {
        // Setup the output xml
        FileOutputStream fos = new FileOutputStream(strOutFile);
        OutputFormat format = OutputFormat.createPrettyPrint();
        out = new XMLWriter(fos, format);

        out.startDocument();
        out.startElement("","","collection",new AttributesImpl());
            }

            // Read through the input records
            Loader handler = new Loader();
            reader.addHandler("/collection/record", handler);
            reader.read(is);

            if (bOutFile) {
        // Close the xml output
        out.endElement("","","collection");
        out.endDocument();
        out.close();
      }
    }

    catch (Exception e) {
      log.error("Uncaught exception: " + ErrorHandling.getStackTrace(e));
      System.exit(1);
    }

    finally {
      log.info("=====================================\n" +
	       "Records read:    " + lRead + "\n" +
	       "Records written: " + lWritten + "\n"
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

      String strTitle = getXPath("/record/title").selectSingleNode(doc).getText();
      log.info("Title: " + strTitle);

      // Fixups
      fixAuthor(doc, "/record/author");
      fixAuthor(doc, "/record/advisors");
      fixKeywords(doc);

      // Transform to dublin core
      DocumentSource source = new DocumentSource(record);
      DocumentResult result = new DocumentResult();

      tDC.transform(source, result);
      
      Document dc = result.getDocument();

      if (bOutFile) {
	      out.write(dc.getRootElement());
      }

      if (bLoad) {
	      loadItem(doc, dc);
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

  public static void loadItem(Document meta, Document dc) {
    Context context = null;

    try {
      // Setup the context
      context = new Context();
      context.setCurrentUser(isreperson);
      context.turnOffAuthorisationSystem();

      // Create a new Item, started in a workspace
      WorkspaceItem wi = workspaceItemService.create(context, isrcollection, false);
      Item item = wi.getItem();

      // Add dublin core
      addDC(context, item, dc);

      // Add bitstreams
      addBitstreams(context, item, meta);

      // Finish installation into the database
      installItemService.installItem(context, wi);

      // Get the handle
      String strHandle = handleService.findHandle(context, item);
      strHandle = handleService.getCanonicalForm(strHandle);

      context.commit();

      lWritten++;

      // Report the created item
      log.info("  written: " + strHandle);
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

  /************************************************************ fixAuthor */

  static Pattern pAuthor = Pattern.compile(", |\\s+and\\s+");
  static Pattern pName = Pattern.compile("(.*)\\s+(\\S+)");

  /**
   * Fix the authors:
   * <ul>
   * <li> split apart multiple authors </li>
   * <li> change to lastname, firstname </li>
   * </ul>
   */

  public static void fixAuthor(Document doc, String strXPath) {
    Element e = (Element)getXPath(strXPath).selectSingleNode(doc);

    Element parent = e.getParent();
    List l = parent.content();
    int n = parent.indexOf(e);

    String strText = e.getText().trim();

    if (strText.equals("NULL")) {
      e.detach();
    }

    // split multiple authors
    String strAuthor[] = pAuthor.split(strText);
    for (int i=0; i < strAuthor.length; i++) {

      // lastname, firstname
      Matcher m = pName.matcher(strAuthor[i]);
      if (m.matches()) {
	      strAuthor[i] = m.group(2) + ", " + m.group(1);
      }

      if (i == 0) {
        e.setText(strAuthor[i]);
            } else {
        Element e2 = e.createCopy();
        e2.setText(strAuthor[i]);
        n++;
        l.add(n, e2);
      }
    }
    
  }


  /*********************************************************** fixKeywords */

  /**
   * Fix the keywords:
   * <ul>
   * <li> eliminate prepended comma </li>
   * </ul>
   */

  public static void fixKeywords(Document doc) {
    Element e = (Element)getXPath("/record/keywords").selectSingleNode(doc);
    String strText = e.getText().trim();

    if (strText.startsWith(",")) {
      strText = strText.substring(1).trim();
    }

    if (strText.equals("")) {
      e.detach();
    } else {
      e.setText(strText);
    }
  }


  /********************************************************* addBitstreams */
  /**
   * Add bitstreams to the item.
   */

  public static void addBitstreams(Context context, Item item, Document meta) throws Exception {

    // Get the ORIGINAL bundle which contains public bitstreams
    List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
    Bundle bundle = null;

    if (bundles.isEmpty()) {
      bundle = bundleService.create(context, item, "ORIGINAL");
    } else {
      bundle = bundles.get(0);
    }

    // Build the file name
    String strFileName = getXPath("/record/filename").selectSingleNode(meta).getText();

    String strDiskName = strPdfDir + "/" + strFileName + ".gz";
    File f = new File(strDiskName);

    if (! f.exists()) {
      throw new Exception("File does not exist: " + strDiskName);
    }

    String strName = strFileName.substring(strFileName.indexOf('/')+1);

    // Create the bitstream
    InputStream is = new GZIPInputStream(new FileInputStream(f));
    Bitstream bs = bitstreamService.create(context, bundle, is);
    bs.setName(context, strName);
    
    // Set the format
    BitstreamFormat bf = bitstreamFormatService.guessFormat(context, bs);
    bs.setFormat(context, bf);
    
    bitstreamService.update(context, bs);
  }


  /***************************************************************** addDC */
  /**
   * Add dubline core to the item
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
	      language = configurationService.getProperty("default.language");
      }

      itemService.addMetadata(context, item, MetadataSchema.DC_SCHEMA, element, qualifier, language, value);
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



