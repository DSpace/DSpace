
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.FilenameFilter;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

// DSpace
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.DCValue;

// XSL
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

// XPath
//import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.FunctionContext;
import org.jaxen.Navigator;
import org.jaxen.SimpleFunctionContext;
import org.jaxen.XPathFunctionContext;

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
import org.dom4j.io.DocumentInputSource;
import org.dom4j.io.DocumentResult;

import org.xml.sax.InputSource;


// Retroactive changes to thesis/dissertation loader: type field

public class RetroThesDissType
{
  public static Context context = null;
  public static Hashtable hExt = new Hashtable();

  static Transformer loaddiss = null;
  static Map namespace = new HashMap();
  static Map mXPath = new HashMap();
  static DocumentFactory df = DocumentFactory.getInstance();
  static SAXReader reader = null;

  public static int nUpdates = 0;
  public static int nItems = 0;

  static Pattern pItems = Pattern.compile("\\d{8}-\\d{6}-\\d{4}");
  static Pattern pMeta = Pattern.compile("(dissertation|umi-umd-\\d*).xml");


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

        // dspace config
        String strDrumDir = ConfigurationManager.getProperty("dspace.dir");
        String strProquestDir = ConfigurationManager.getProperty("proquest.dir") + "/items/processed/success";

        // the xml parser
        reader = new SAXReader();

        // the transformer
        TransformerFactory tFactory = TransformerFactory.newInstance();
        loaddiss = tFactory.newTransformer(new StreamSource(new File(strDrumDir + "/load/load-diss.xsl")));     

        // Load files
        loadFiles(strProquestDir);

        // Load the collections
        System.out.print("Loading collections...");
        Collection collUm = null;
        Collection colls[] = Collection.findAll(context);
        for (int i=0; i < colls.length; i++) {
          if (colls[i].getMetadata("name").equals("UM Theses and Dissertations")) {
            collUm = colls[i];
          }
        }

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
    
    // Load the metadata
    Document docMD = reader.read(new InputSource(open((File)hExt.get(strHandle))));
    
    // Transform to dublin core

    DocumentSource source = new DocumentSource(docMD);
    DocumentResult result = new DocumentResult();
        
    loaddiss.transform(source, result);

    Document docDC = result.getDocument();
    
    // Get the relevant information
    String strType = getXPath("/dublin_core/dcvalue[@element='type' and @qualifier='none']").selectSingleNode(docDC).getText();
    
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
    
    // Check for updates
    if (bUpdated) {
    	item.update();
    	nUpdates++;
    } else {
    	System.out.println("    No update needed");
    }
    nItems++;
  }


  /***************************************************************** open */
  
  public static BufferedReader open(File file) 
    throws IOException
  {
    return new BufferedReader(new InputStreamReader(new FileInputStream(file)));
  }


  /************************************************************ loadFiles */

  public static void loadFiles(String strProquestDir)
    throws Exception
  {
    // Load the extract information
    System.out.print("Loading files...");

    File pdir = new File(strProquestDir);

    // Get the item dirs
    File dirs[] = pdir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          Matcher m = pItems.matcher(name);
          return m.matches();
        }
      });
        
    // Loop through the item dirs
    for (int i=0; i < dirs.length; i++) {
      File dir = dirs[i];

      String strHandle = getHandle(dir);

      // Get the proquest xml file
      File files[] = dir.listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            Matcher m = pMeta.matcher(name);
            return m.matches();
          }
        });

      if (files.length == 0) {
        System.out.println("  No proquest metadata file found");
        continue;
      } else if (files.length > 1) {
        System.out.println("  Too many proquest metadata files found");
        continue;
      }

      // Load the info
      hExt.put(strHandle, files[0]);
    }

    System.out.println("  " + hExt.size());
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



