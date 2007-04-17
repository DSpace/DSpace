
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.DCValue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;

import javax.xml.transform.dom.DOMSource;  
import javax.xml.transform.dom.DOMResult;

import javax.xml.transform.stream.StreamSource; 
import javax.xml.transform.stream.StreamResult; 

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// Retroactive changes to thesis/dissertation loader

public class RetroThesDiss
{
  public static Context context = null;
  public static Hashtable hExt = new Hashtable();
  public static Hashtable hColls = new Hashtable();
  public static Transformer tImage = null;
  public static DocumentBuilder db = null;
  public static Transformer loaddiss = null;
  public static Transformer mapcontact = null;
  public static XPath xp = null;
  public static XPathExpression xpe = null;
  public static XPathExpression xpeType = null;
  public static XPathExpression xpeAdvisor = null;
  public static XPathExpression xpeSubject = null;
  public static XPathExpression xpeDepartment = null;
  public static int nUpdates = 0;
  public static int nItems = 0;


  /***************************************************************** main */

  public static void
  main(String[] args)
    throws Exception
    {
      PropertyConfigurator.configure("log4j.properties");

      try {

	// Setup the context
	context = new Context();
	context.setIgnoreAuthorization(true);

	// Get command line
	String strDrumDir = args[0];
	String strExtDir = args[1];

	// Load files
	loadFiles(strDrumDir, strExtDir);

	// Load the collections
	System.out.print("Loading collections...");
	Collection collUm = null;
	Collection colls[] = Collection.findAll(context);
	for (int i=0; i < colls.length; i++) {
	  if (colls[i].getMetadata("name").equals("UM Theses and Dissertations")) {
	    collUm = colls[i];
	  }
	  hColls.put(colls[i].getMetadata("name"), colls[i]);
	}
	System.out.println("  " + hColls.size());

	// Create DOM/SAX/XSLT environment
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	dbf.setNamespaceAware(true);
	//dbf.setValidating(true);
	db = dbf.newDocumentBuilder();

	TransformerFactory tFactory = TransformerFactory.newInstance();
	loaddiss = tFactory.newTransformer(new StreamSource(new File(strDrumDir + "/load/load-diss.xsl")));	
	mapcontact = tFactory.newTransformer(new StreamSource(new File(strDrumDir + "/load/load-diss-map-contact.xsl")));	
	
	XPathFactory xpf = XPathFactory.newInstance();
	xp = xpf.newXPath();

	// Read the Theses/Dissertation items
	Vector vNoMatch = new Vector();

	if (collUm != null) {
	  System.out.println("\nReading items in " + collUm.getMetadata("name") + "...");
	  for (ItemIterator i = collUm.getItems(); i.hasNext(); ) {
	    Item item = i.next();
	
	    String strHandle = item.getHandle();
	    String title = (item.getDC("title", null, Item.ANY))[0].value;
	    if (title.length() > 60) {
	      title = title.substring(0,60);
	    }

	    // Look for handle match
	    if (!hExt.containsKey(strHandle)) {
	      // Save
	      vNoMatch.add(strHandle + ": " + title);

	    } else {
	      handleItem(item, strHandle, title);
	    }
	  }
	  
	}
	context.commit();
	context.complete();
      }
      finally {
	System.out.println("\nTotal items:   " + nItems);
	System.out.println("Items updated: " + nUpdates);
      }
    }


  /*********************************************************** handleItem */

  public static void handleItem(Item item, String strHandle, String strTitle) 
    throws Exception
  {
    boolean bUpdated = false;

    System.out.println("\n  " + strHandle + ": " + strTitle);

    if (xpeType == null) {
      xpeType = xp.compile("/dublin_core/dcvalue[@element='type' and @qualifier='none']");
      xpeAdvisor = xp.compile("/dublin_core/dcvalue[@element='contributor' and @qualifier='advisor']");
      xpeSubject = xp.compile("/dublin_core/dcvalue[@element='subject' and @qualifier='none']");
      xpeDepartment = xp.compile("/dublin_core/dcvalue[@element='contributor' and @qualifier='department']");
    }

    // Load the metadata
    Document docMD = db.parse(new InputSource(open((File)hExt.get(strHandle))));

    // Transform to dublin core
    DOMResult dr = new DOMResult();
    loaddiss.transform(new DOMSource(docMD), dr);
    Document docDC = (Document)dr.getNode();

    // Get the relevant information
    String strType = xpeType.evaluate(docDC);

    NodeList nlDepartment   = (NodeList) xpeDepartment.evaluate(docDC, XPathConstants.NODESET);
    String strDepartment[] = new String[nlDepartment.getLength()];
    for (int i =0; i < strDepartment.length; i++) {
      strDepartment[i] = nlDepartment.item(i).getFirstChild().getNodeValue();
    }

    NodeList nlAdvisor   = (NodeList) xpeAdvisor.evaluate(docDC, XPathConstants.NODESET);
    String strAdvisor[] = new String[nlAdvisor.getLength()];
    for (int i =0; i < strAdvisor.length; i++) {
      strAdvisor[i] = nlAdvisor.item(i).getFirstChild().getNodeValue();
    }

    NodeList nlSubject   = (NodeList) xpeSubject.evaluate(docDC, XPathConstants.NODESET);
    String strSubject[] = new String[nlSubject.getLength()];
    for (int i =0; i < strSubject.length; i++) {
      strSubject[i] = nlSubject.item(i).getFirstChild().getNodeValue();
    }

    //System.out.println("    type=" + strType);
    //System.out.println("    advisor=" + strAdvisor);
    //System.out.println("    subject=" + Arrays.asList(strSubject));
    //System.out.println("    department=" + strDepartment);

    // Check for needed updates
    DCValue dc[] = null;

    // type
    dc = item.getDC("type", null, Item.ANY);
    if (dc == null || dc.length != 1) {
      System.out.println("    Error: unable to get existing dc:type");
    } else if (!dc[0].value.equals(strType.trim())) {
      System.out.println("    Updating dc:type to " + strType);
      item.clearDC("type", null, Item.ANY);
      item.addDC("type", null, "en_US", strType);
      bUpdated = true;
    }

    // contributor.department
    dc = item.getDC("contributor", "department", Item.ANY);
    boolean bNeedsDepartment = false;
    if (dc == null || dc.length != strDepartment.length) {
      bNeedsDepartment = true;
    } else {
      for (int j=0; j < dc.length; j++) {
	if (! dc[j].value.equals(strDepartment[j].trim())) {
	  bNeedsDepartment = true;
	}
      }
    }
    if (bNeedsDepartment) {
      System.out.println("    Adding dc:contributor.department of " + Arrays.asList(strDepartment));
      item.clearDC("contributor", "department", "en_US");
      item.addDC("contributor", "department", "en_US", strDepartment);
      bUpdated = true;
    }

    // contributor.advisor
    dc = item.getDC("contributor", "advisor", Item.ANY);
    boolean bNeedsAdvisor = false;
    if (strAdvisor.length == 0) {
      System.out.println("    Error: no dc:contributor.advisor");
    }
    if (dc == null || dc.length != strAdvisor.length) {
      bNeedsAdvisor = true;
    } else {
      for (int j=0; j < dc.length; j++) {
	if (! dc[j].value.equals(strAdvisor[j].trim())) {
	  bNeedsAdvisor = true;
	}
      }
    }
    if (bNeedsAdvisor) {
      System.out.println("    Adding dc:contributor.advisor of " + Arrays.asList(strAdvisor));
      item.clearDC("contributor", "advisor", "en_US");
      item.addDC("contributor", "advisor", "en_US", strAdvisor);
      bUpdated = true;
    }

    // subject
    dc = item.getDC("subject", null, Item.ANY);
    boolean bNeedsSubject = false;
    if (strSubject.length == 0) {
      System.out.println("    Error: no dc:subject");
    }
    if (dc == null || dc.length != strSubject.length) {
      bNeedsSubject = true;
    } else {
      for (int j=0; j < dc.length; j++) {
	if (! dc[j].value.equals(strSubject[j].trim())) {
	  bNeedsSubject = true;
	}
      }
    }
    if (bNeedsSubject) {
      System.out.println("    Adding dc:subject of " + Arrays.asList(strSubject));
      item.clearDC("subject", null, "en_US");
      item.addDC("subject", null, "en_US", strSubject);
      bUpdated = true;
    }

    // Do collection mapping
    if (handleItemCollections(item, docMD)) {
      bUpdated = true;
    }

    // Check for updates
    if (bUpdated) {
      item.update();
      nUpdates++;
    } else {
      System.out.println("    No update needed");
    }
    nItems++;
  }


  /************************************************ handleItemCollections */

  public static boolean handleItemCollections(Item item, Document docMD)
    throws Exception
  {
    boolean bMap = false;
    boolean bUpdated = false;

    // Transform to list of mapped collections
    StringWriter sw = new StringWriter();
    mapcontact.transform(new DOMSource(docMD), new StreamResult(sw));
    BufferedReader br = new BufferedReader(new StringReader(sw.toString()));

    // Loop through the mapped collections
    String strCollName = null;
      
    while ((strCollName = br.readLine()) != null) {
      bMap = true;
      if (!hColls.containsKey(strCollName)) {
	System.out.println("    Error: unable to map to collection: " + strCollName);
      } else {
	Collection coll = (Collection)hColls.get(strCollName);

	// Loop through the item's existing collections
	Collection colls[] = item.getCollections();
	boolean bInCollection = false;
	for (int i=0; i < colls.length; i++) {
	  if (coll.equals(colls[i])) {
	    bInCollection = true;
	  }
	}

	if (!bInCollection) {
	  // Add it to the collection
	  coll.addItem(item);
	  System.out.println("    Mapping to collection: " + strCollName);
	  bUpdated = true;
	}
      }
    }

    if (!bMap) {
      System.out.println("    Error: no mapped collections");
    }

    return bUpdated;
  }


  /***************************************************************** open */
  
  public static BufferedReader open(File file) 
    throws IOException
  {
    return new BufferedReader(new InputStreamReader(new FileInputStream(file)));
  }


  /************************************************************ loadFiles */

  public static void loadFiles(String strDrumDir, String strExtDir)
    throws Exception
  {
    // Load the extract information
    System.out.print("Loading extract files...");

    File fExtDir = new File(strExtDir);

    // Get the directories in ExtDir
    File f[] = fExtDir.listFiles();
    for (int i=0; i < f.length; i++) {
      if (f[i].isDirectory()) {
	File x[] = f[i].listFiles();

	String strHandle = null;
	File md = null;

	// Read the files in the directory
	for (int j=0; j < x.length; j++) {
	  String n = x[j].getName();

	  if (n.equals("mapfile")) {
	    // Get the handle
	    BufferedReader br = open(x[j]);
	    String s = null;
	    if ((s = br.readLine()) != null) {
	      strHandle = s.substring(5);
	    }
	    br.close();
	    
	  } else if ((n.startsWith("umi-umd-") && n.endsWith(".xml")) ||
		     n.equals("dissertation.xml")) {
	    md = x[j];
	  }
	}

	// Load the info
	if (strHandle != null && md != null) {
	  hExt.put(strHandle, md);
	}
      }
    }

    System.out.println("  " + hExt.size());
  }


  /********************************************************* xmlNormalize */
  /** 
   * Normalizes the given string.
   */

  public static String 
    xmlNormalize(String s) 
  {
    StringBuffer str = new StringBuffer();

    //s = xmlStripControl(s);

    int len = (s != null) ? s.length() : 0;
    for ( int i = 0; i < len; i++ ) {
      char ch = s.charAt(i);

      switch ( ch ) {
      case '<': {
        str.append("&lt;");
        break;
      }
      case '>': {
        str.append("&gt;");
        break;
      }
      case '&': {
        str.append("&amp;");
        break;
      }
      case '"': {
        str.append("&quot;");
        break;
      }
      case '\'': {
        str.append("&apos;");
        break;
      }
      default: {
        str.append(ch);
      }
      }
    }

    return(str.toString());

  }

}



