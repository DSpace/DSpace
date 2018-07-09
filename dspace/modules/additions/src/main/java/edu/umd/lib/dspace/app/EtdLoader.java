/*
 * Copyright (c) 2006 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lib.dspace.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.InvalidXPathException;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dspace.authorize.AuthorizeServiceImpl;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.EtdUnit;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EtdUnitService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.sort.OrderFormat;
import org.dspace.util.UUIDUtils;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.Record;
import org.xml.sax.InputSource;

import edu.umd.lims.util.ErrorHandling;
// SQL
// IO
// XML
// XSL
// XPath
// Log4J
// DSpace
// Marc4J
// Lims

/*********************************************************************
 * ETD Loader. Algorithm:
 * 
 * <pre>
 *    get params
 *    get properties
 *    open zipfile
 *    open marc file
 *    read items
 *    foreach item
 *      xsl metadata to dublin core
 *      map additional collections
 *      foreach file
 *        add Bitstream
 *      add item
 *      add embargo to bitstreams
 *      save to marc file
 *      if duplicate title send email notice
 *      if no mapped collections send email notice
 * </pre>
 * 
 * @author Ben Wallberg
 *********************************************************************/

public class EtdLoader
{

    private static Logger log = Logger.getLogger(EtdLoader.class);

    final static int MAX_WORD_COUNT = 4;

    static long lRead = 0;

    static long lWritten = 0;

    static long lEmbargo = 0;

    static SAXReader reader = new SAXReader();

    static Transformer tDC = null;

    static Transformer tMeta2Marc = null;

    static Map namespace = new HashMap();

    static Map mXPath = new HashMap();

    static DocumentFactory df = DocumentFactory.getInstance();

    static Collection etdcollection = null;

    static EPerson etdeperson = null;

    static SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");

    static MarcStreamWriter marcwriter = null;

    static PrintWriter csvwriter = null;

    static Pattern pZipEntry = Pattern
            .compile(".*_umd_0117._(\\d+)(.pdf|_DATA.xml)");

    
    private final static ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    
    private final static HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    private final static CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    
    private final static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    
    private final static BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    
    private final static BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    private final static BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();

    private final static WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

    private final static InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();

    private final static EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();

    private final static GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    private final static ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();

    private final static AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    protected static EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();

    /***************************************************************** main */
    /**
     * Command line interface.
     */

    public static void main(String args[]) throws Exception
    {

        try
        {

            // Properties
            Properties props = System.getProperties();
            String strZipFile = props.getProperty("etdloader.zipfile", null);
            String strSingleItem = props.getProperty("etdloader.singleitem",
                    null);
            String strMarcFile = props.getProperty("etdloader.marcfile", null);
            String strCsvFile = props.getProperty("etdloader.csvfile", null);

            // dspace dir
            String strDspace = configurationService.getProperty("dspace.dir");
            String strEPerson = configurationService
                    .getProperty("drum.etdloader.eperson");
            String strCollection = configurationService
                    .getProperty("drum.etdloader.collection");

            String transferMarc = configurationService
                    .getProperty("drum.etdloader.transfermarc");

            log.info("DSpace directory : " + strDspace);
            log.info("ETD Loaeder Eperson : " + strEPerson);
            log.info("ETD Loader Collection: " + strCollection);
            log.info("ETD Loader Transfer Marc: " + transferMarc);

            // the transformers
            TransformerFactory tFactory = TransformerFactory.newInstance();
            tDC = tFactory.newTransformer(new StreamSource(new File(strDspace
                    + "/config/load/etd2dc.xsl")));
            tMeta2Marc = tFactory.newTransformer(new StreamSource(new File(
                    strDspace + "/config/load/etd2marc.xsl")));

            // open the marc output file
            if (strMarcFile != null)
            {
                FileOutputStream fos = new FileOutputStream(new File(
                        strMarcFile), true);
                marcwriter = new MarcStreamWriter(fos, "UTF-8");
            }

            // open the csv output file
            if (strCsvFile != null)
            {
                FileOutputStream fos = new FileOutputStream(strCsvFile, true);
                OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                csvwriter = new PrintWriter(osw);
            }

            // Get DSpace values
            Context context = new Context();

            if (strCollection == null)
            {
                throw new Exception("drum.etdloader.collection not set");
            }
            etdcollection = collectionService.find(context,
                    UUIDUtils.fromString(strCollection));
            if (etdcollection == null)
            {
                throw new Exception("Unable to find etdloader.collection: "
                        + strCollection);
            }

            if (strEPerson == null)
            {
                throw new Exception("drum.etdloader.eperson not set");
            }
            etdeperson = epersonService.findByEmail(context, strEPerson);
            if (etdeperson == null)
            {
                throw new Exception("Unable to find etdloader.eperson: "
                        + strEPerson);
            }

            // Open the zipfile
            ZipFile zip = new ZipFile(new File(strZipFile), ZipFile.OPEN_READ);

            // Get the list of entries
            Map map = readItems(zip);
            log.info("Found " + map.size() + " item(s)");

            // Process each entry
            for (Iterator i = map.keySet().iterator(); i.hasNext();)
            {
                String strItem = (String) i.next();

                lRead++;

                if (strSingleItem == null || strSingleItem.equals(strItem))
                {
                    loadItem(zip, strItem, (List) map.get(strItem));
                }
            }

            context.complete();
        }

        catch (Exception e)
        {
            log.error("Uncaught exception: " + ErrorHandling.getStackTrace(e));
        }

        finally
        {
            if (marcwriter != null)
            {
                try
                {
                    marcwriter.close();
                }
                catch (Exception e)
                {
                }
            }

            if (csvwriter != null)
            {
                try
                {
                    csvwriter.close();
                }
                catch (Exception e)
                {
                }
            }

            log.info("=====================================\n"
                    + "Records read:    " + lRead + "\n" + "Records written: "
                    + lWritten + "\n" + "Embargoes:       " + lEmbargo);
        }
    }

    /******************************************************** addBitstreams */
    /**
     * Add bitstreams to the item.
     */

    public static void addBitstreams(Context context, Item item, ZipFile zip,
            List files) throws Exception
    {

        // Get the ORIGINAL bundle which contains public bitstreams
        Bundle originalBundle = getBundle(context, item, "ORIGINAL");
        // Get the METADATA bundle 
        Bundle metaBundle = getBundle(context, item, "METADATA");

        // Add the Proquest ETD metadata XML to the METADATA bundle 
        String strFileName = (String) files.get(0);
        ZipEntry ze = (ZipEntry) files.get(1);
        createBitstream(context, metaBundle, strFileName, zip.getInputStream(ze));

        // Add the content bitstreams to ORIGINAL bundle 
        // Loop through the files
        for (int i = 2; i < files.size(); i += 2)
        {
            strFileName = (String) files.get(i);
            ze = (ZipEntry) files.get(i + 1);

            createBitstream(context, originalBundle, strFileName, zip.getInputStream(ze));
        }
    }

    public static Bundle getBundle(Context context, Item item, String type) throws Exception {
      List<Bundle> bundles = itemService.getBundles(item, type);
      Bundle bundle = null;

      if (bundles.isEmpty())
      {
          bundle = bundleService.create(context, item, type);
      }
      else
      {
          bundle = bundles.get(0);
      }
      return bundle;
    }

    public static void createBitstream(Context context, Bundle bundle, String name, InputStream stream)
      throws Exception {
        log.debug("Adding bitstream for " + name);
      
        // Create the bitstream
        Bitstream bs = bitstreamService.create(context, bundle, stream);
        bs.setName(context, name);

        // Set the format
        BitstreamFormat bf = bitstreamFormatService.guessFormat(context, bs);
        bs.setFormat(context, bf);

        bitstreamService.update(context, bs);

    }

    /***************************************************************** addDC */
    /**
     * Add dubline core to the item
     */

    public static void addDC(Context context, Item item, Document meta)
            throws Exception
    {

        // Transform to dublin core
        DocumentSource source = new DocumentSource(meta);
        DocumentResult result = new DocumentResult();

        tDC.transform(source, result);

        Document dc = result.getDocument();

        if (log.isDebugEnabled())
        {
            log.debug("dublin core:\n" + toString(dc));
        }

        // Loop through the elements
        List l = getXPath("/dublin_core/dcvalue").selectNodes(dc);
        for (Iterator i = l.iterator(); i.hasNext();)
        {
            Node ndc = (Node) i.next();

            String value = ndc.getText();

            String element = getXPath("@element").selectSingleNode(ndc)
                    .getText();

            Node n = getXPath("@qualifier").selectSingleNode(ndc);
            String qualifier = ((n == null || n.getText().equals("none")) ? null
                    : n.getText());

            n = getXPath("@language").selectSingleNode(ndc);
            String language = ((n == null || n.getText().equals("none")) ? null
                    : n.getText());
            if (language == null)
            {
                language = configurationService.getProperty("default.language");
            }

            itemService.addMetadata(context, item, MetadataSchema.DC_SCHEMA, element, qualifier, language, value);
            log.debug(element + ":" + qualifier + ":" + language + ":" + value);
        }
    }

    /************************************************************ addEmbargo */
    /**
     * Add embargo to the bitstreams.
     */

    static Group etdgroup = null;

    static Group anongroup = null;

    public static void addEmbargo(Context context, Item item, String strEmbargo)
            throws Exception
    {

        log.debug("Adding embargo policies");

        // Get groups
        if (anongroup == null)
        {
            anongroup = groupService.findByName(context, "Anonymous");
            if (anongroup == null)
            {
                throw new Exception("Unable to find Anonymous group");
            }
        }

        if (etdgroup == null)
        {
            etdgroup = groupService.findByName(context, "ETD Embargo");
            if (etdgroup == null)
            {
                throw new Exception("Unable to find ETD Embargo group");
            }
        }

        // Setup the policies
        List lPolicies = new ArrayList();
        ResourcePolicy rp = null;

        if (strEmbargo.equals("never"))
        {
            log.info("Embargoed forever");
            rp = resourcePolicyService.create(context);
            rp.setAction(Constants.READ);
            rp.setGroup(etdgroup);
            lPolicies.add(rp);
        }
        else
        {
            Date date = format.parse(strEmbargo);
            log.info("Embargoed until " + date);

            rp = resourcePolicyService.create(context);
            rp.setAction(Constants.READ);
            rp.setGroup(etdgroup);
            rp.setEndDate(date);
            lPolicies.add(rp);

            rp = resourcePolicyService.create(context);
            rp.setAction(Constants.READ);
            rp.setGroup(anongroup);
            rp.setStartDate(date);
            lPolicies.add(rp);
        }

        // Loop through the bitstreams
        List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
        Bundle bundle = bundles.get(0);

        List<Bitstream> bs = bundle.getBitstreams();
        for (Bitstream bitstream : bs)
        {
            // Set the policies
            authorizeService.removeAllPolicies(context, bitstream);
            authorizeService.addPolicies(context, lPolicies, bitstream);
        }
    }

    /*********************************************************** checkTitle */
    /**
     * Check for duplicate titles.
     */

    private static void checkTitle(Context c, Item item, Set sCollections)
            throws Exception
    {

        // Get the title(s)
        List<MetadataValue> metadataValues = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);

        // Process each title
        for (MetadataValue metadataValue : metadataValues)
        {

            // Get normalized title
            String title = OrderFormat.makeSortString(metadataValue.getValue(),
                    metadataValue.getLanguage(), OrderFormat.TITLE);

            log.debug("checking for duplicate title: " + title);

            log.debug("Title Length: " + title.length());

            // Execute browse, with one result
            BrowseIndex bi = BrowseIndex.getBrowseIndex("title");
            log.debug("Browse Index Title Count" + bi.getMetadataCount());

            BrowserScope scope = new BrowserScope(c);

            scope.setBrowseIndex(bi);
            scope.setResultsPerPage(10);

            String[] words = title.trim().split(" ");

            StringBuilder searchTerm = new StringBuilder();

            for (int wordCount = 0; (wordCount < MAX_WORD_COUNT && wordCount < words.length); wordCount++)
            {
                searchTerm = searchTerm.append(words[wordCount]);
                searchTerm = searchTerm.append(" ");
            }

            String searchTitle = "";

            int index = title.indexOf(" ", 1);

            if (searchTerm.length() > 0)
            {
                searchTitle = searchTerm.toString().trim();
            }
            else
            {
                searchTitle = title.substring(0, index);
            }

            log.debug("Search Term: " + searchTerm.toString().trim()
                    + "Search Term Length" + searchTerm.length());
            log.debug("Search Title " + searchTitle + "Search Title Length: "
                    + searchTitle.length());

            scope.setStartsWith(searchTitle);

            BrowseEngine be = new BrowseEngine(c);
            BrowseInfo binfo = be.browse(scope);

            log.debug("Browse Info Result Item Count: "
                    + binfo.getResultCount() + ": Total count : = "
                    + binfo.getTotal());

            log.debug("Sort Option: " + binfo.getSortOption().getName());

            int biItemCount = 0;

            for (Iterator j = binfo.getResults().iterator(); j.hasNext();)
            {
                Item bitem = (Item) j.next();

                // Get normalized title for browse item
                String btitle = OrderFormat.makeSortString(bitem.getName(),
                        null, OrderFormat.TITLE);

                log.debug("Current Loaded Title: " + title + "; Title ID: "
                        + item.getID() + "; Title Name:" + item.getName()
                        + "; Search title " + searchTitle);

                log.debug("Browsed Title: " + btitle + ";Browsed Title ID: "
                        + bitem.getID() + "; Browsed Title Name:"
                        + bitem.getName());

                biItemCount = biItemCount + 1;

                log.debug("Browsed Items Count: " + biItemCount);

                // Check for the match
                if (title.equals(btitle) && item.getID() != bitem.getID())
                {
                    log.debug("Duplicate title: " + title);

                    // Get the list of collections for the loaded item
                    StringBuffer sbCollections = new StringBuffer();
                    sbCollections.append(etdcollection.getName());
                    for (Iterator ic = sCollections.iterator(); ic.hasNext();)
                    {
                        Collection coll = (Collection) ic.next();
                        sbCollections.append(", ");
                        sbCollections.append(coll.getName());
                    }

                    // Get the email recipient
                    String email = configurationService
                            .getProperty("drum.mail.duplicate_title");
                    if (email == null)
                    {
                        email = configurationService.getProperty("mail.admin");
                    }

                    try
                    {
                        if (email != null)
                        {
                            // Send the email
                            Email bean = ConfigurationManager.getEmail(I18nUtil
                                    .getEmailFilename(
                                            I18nUtil.getDefaultLocale(),
                                            "duplicate_title"));
                            bean.addRecipient(email);
                            bean.addArgument(title);
                            bean.addArgument("" + item.getID());
                            bean.addArgument(handleService.findHandle(c, item));
                            bean.addArgument(sbCollections.toString());
                            bean.send();
                        }
                    }
                    catch (Exception e)
                    {
                        log.error("Cannot send email about detected duplicate title: "
                                + title + "\n" + ErrorHandling.getStackTrace(e));
                    }

                    return;
                }
            }
        }
    }

    /*********************************************************** createMarc */
    /**
     * Create a marc record from the etd metadata.
     */

    public static void createMarc(Document meta, String strHandle, List files)
            throws Exception
    {

        if (marcwriter != null)
        {
            log.debug("Creating marc");

            // Convert etd metadata to marc xml
            DocumentSource source = new DocumentSource(meta);
            DocumentResult result = new DocumentResult();

            tMeta2Marc.setParameter("files", getFileTypes(files));
            tMeta2Marc.setParameter("handle", strHandle);
            tMeta2Marc.transform(source, result);

            // Convert marc xml to marc
            StringWriter sw = new StringWriter();
            XMLWriter writer = new XMLWriter(sw);
            writer.write(result.getDocument());
            writer.flush();
            InputSource is = new InputSource(new StringReader(sw.toString()));
            MarcXmlReader convert = new MarcXmlReader(is);
            Record record = convert.next();

            // Write out the marc record
            marcwriter.write(record);
        }
    }

    /************************************************************ createCsv */
    /**
     * Add an entry in the CSV files for this item: title, author, handle
     */

    public static void createCsv(Item item, String strHandle) throws Exception
    {

        if (csvwriter != null)
        {
            log.debug("Creating CSV");

            // Build the line
            StringBuffer sb = new StringBuffer();

            sb.append('"');

            // Get the title(s)
            List<MetadataValue> titles = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);

            for (int i = 0; i < titles.size(); i++)
            {
                if (i > 0)
                {
                    sb.append("; ");
                }

                sb.append(titles.get(i).getValue().replaceAll("\"", "\"\""));
            }

            sb.append("\",\"");

            // Get the author(s)
            List<MetadataValue> authors = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "contributor", "author", Item.ANY);

            for (int i = 0; i < authors.size(); i++)
            {
                if (i > 0)
                {
                    sb.append("; ");
                }

                sb.append(authors.get(i).getValue().replaceAll("\"", "\"\""));
            }

            sb.append("\",\"");

            // Add the handle
            sb.append(strHandle);

            sb.append('"');

            // Write out the csv line
            csvwriter.println(sb.toString());
        }
    }

    /******************************************************* getCollections */
    /**
     * Get additional mapped collections.
     */

    public static Set<Collection> getCollections(Context context, Document meta)
            throws Exception
    {
        Set<Collection> collections = new HashSet<>();

        log.debug("Looking for mapped collections");

        List l = getXPath(
                "/DISS_submission/DISS_description/DISS_institution/DISS_inst_contact")
                .selectNodes(meta);
        for (Iterator i = l.iterator(); i.hasNext();)
        {

            Node n = (Node) i.next();
            String strDepartment = n.getText().trim().replaceAll(" +", " ");

            log.debug("Found DISS_inst_contact: " + strDepartment);

            EtdUnit etdunit = etdunitService.findByName(context, strDepartment);

            if (etdunit == null)
            {
                log.error("Unable to lookup mapped collection: "
                        + strDepartment);

            }
            else
            {
                collections.addAll(etdunit.getCollections());
            }
        }
        return collections;
    }

    /*********************************************************** getEmbargo */
    /**
     * Get embargo information.
     */

    public static String getEmbargo(Document meta)
    {
        String strEmbargo = null;

        Node n = getXPath(
                "/DISS_submission/DISS_restriction/DISS_sales_restriction[@code=\"1\"]")
                .selectSingleNode(meta);

        if (n != null)
        {
            Node n2 = getXPath("@remove").selectSingleNode(n);
            if (n2 != null && !"".equals(n2.getText()))
            {
                strEmbargo = n2.getText();
            }
            else
            {
                strEmbargo = "never";
            }
        }

        if (strEmbargo != null)
        {
            log.debug("Item is embargoed; remove restrictions " + strEmbargo);
        }

        return strEmbargo;
    }

    /********************************************************** getFileTypes */
    /**
   */

    public static String getFileTypes(List files) throws IOException
    {

        HashSet h = new HashSet();

        // Loop through the files, extracting extensions
        for (int i = 2; i < files.size(); i += 2)
        {
            String strFileName = (String) files.get(i);

            int n = strFileName.lastIndexOf('.');
            if (n > -1)
            {
                h.add(strFileName.substring(n + 1).trim().toLowerCase());
            }
        }

        if (h.contains("mp3"))
        {
            return "Text and audio.";
        }
        else if (h.contains("jpg"))
        {
            return "Text and images.";
        }
        else if (h.contains("xls"))
        {
            return "Text and spreadsheet.";
        }
        else if (h.contains("wav"))
        {
            return "Text and video.";
        }
        else
        {
            return "Text.";
        }
    }

    /************************************************************* getXPath */
    /**
     * Get a compiled XPath object for the expression. Cache.
     */

    private static XPath getXPath(String strXPath) throws InvalidXPathException
    {

        XPath xpath = null;

        if (mXPath.containsKey(strXPath))
        {
            xpath = (XPath) mXPath.get(strXPath);

        }
        else
        {
            xpath = df.createXPath(strXPath);
            xpath.setNamespaceURIs(namespace);
            mXPath.put(strXPath, xpath);
        }

        return xpath;
    }

    /************************************************************* loadItem */
    /**
     * Load one item into DSpace.
     */

    public static void loadItem(ZipFile zip, String strItem, List files)
    {
        log.info("=====================================\n" + "Loading item "
                + strItem + ": " + ((files.size() / 2) - 1) + " bitstream(s)");

        Context context = null;

        try
        {
            // Setup the context
            context = new Context();
            context.setCurrentUser(etdeperson);
            context.turnOffAuthorisationSystem();

            // Read the ETD metadata
            ZipEntry ze = (ZipEntry) files.get(1);
            Document meta = reader
                    .read(new InputSource(zip.getInputStream(ze)));
            if (log.isDebugEnabled())
            {
                log.debug("ETD metadata:\n" + toString(meta));
            }

            // Map to additional collections
            Set<Collection> sCollections = getCollections(context, meta);

            // Create a new Item, started in a workspace
            WorkspaceItem wi = workspaceItemService.create(context, etdcollection,
                    false);
            Item item = wi.getItem();

            // Add dublin core
            addDC(context, item, meta);

            // Add mapped collections
            wi.addMappedCollections(new ArrayList<Collection>(sCollections));

            // Add bitstreams
            addBitstreams(context, item, zip, files);

            // Finish installation into the database
            installItemService.installItem(context, wi);

            // Add embargo
            String strEmbargo = getEmbargo(meta);
            if (strEmbargo != null)
            {
                addEmbargo(context, item, strEmbargo);
                lEmbargo++;
            }

            // Get the handle
            String strHandle = handleService.findHandle(context, item);
            strHandle = handleService.getCanonicalForm(strHandle);

            context.commit();

            lWritten++;

            // Report the created item
            reportItem(context, item, strHandle, sCollections);

            // Check for duplicate titles
            checkTitle(context, item, sCollections);

            // Report missing collections
            if (sCollections.size() == 0)
            {
                reportCollections(context, item);
            }

            // Create marc record for upload to TSD
            createMarc(meta, strHandle, files);

            // Create csv entry for this item
            createCsv(item, strHandle);
        }

        catch (Exception e)
        {
            log.error("Error loading item " + strItem + ": "
                    + ErrorHandling.getStackTrace(e));
            if (context != null)
            {
                context.abort();
            }
        }

        finally
        {
            if (context != null)
            {
                try
                {
                    context.complete();
                }
                catch (Exception e)
                {
                }
            }
        }
    }

    /********************************************************** readItems */
    /**
     * Read and compile the entries from the zip file. Return a map; the key is
     * the item number, the value is list of file name/ZipEntry pairs with the
     * first entry being the metadata and the second entry being the primary
     * pdf. Note that each zip file now contains only one ETD item.
     */

    public static Map readItems(ZipFile zip)
    {

        String strItem = null;

        ArrayList lmap = new ArrayList();
        lmap.add(0, new Object());
        lmap.add(1, new Object());
        lmap.add(2, new Object());
        lmap.add(3, new Object());

        log.info("Reading " + zip.size() + " zip file entries");

        // Loop through the entries
        for (Enumeration e = zip.entries(); e.hasMoreElements();)
        {
            ZipEntry ze = (ZipEntry) e.nextElement();
            String strName = ze.getName();

            log.debug("zip entry: " + strName);

            // skip directories
            if (ze.isDirectory())
            {
                continue;
            }

            // split into path components
            String s[] = strName.split("/");

            String strFileName = s[s.length - 1];

            Matcher m = pZipEntry.matcher(s[0]);
            if (m.matches())
            {

                // Get the item number
                if (strItem == null)
                {
                    strItem = m.group(1);

                    log.debug("item number is " + strItem);
                }

                // Put the file in the right position
                if (strFileName.endsWith("_DATA.xml"))
                {
                    lmap.set(0, strFileName);
                    lmap.set(1, ze);
                }

                else if (strFileName.endsWith(".pdf"))
                {
                    lmap.set(2, strFileName);
                    lmap.set(3, ze);
                }
            }

            else
            {
                lmap.add(strFileName);
                lmap.add(ze);
            }
        }

        Map map = new TreeMap();
        map.put(strItem, lmap);

        return map;
    }

    /**************************************************** reportCollections */
    /**
     * Report missing mapped collections
     */

    public static void reportCollections(Context context, Item item)
            throws Exception
    {
        // Get the title(s)
        String strTitle= itemService.getMetadataFirstValue(item, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);

        // Get the email recipient
        String email = configurationService.getProperty("drum.mail.etd.recipient");
        if (email == null)
        {
            email = configurationService.getProperty("mail.admin");
        }

        if (email != null)
        {
            // Send the email
            Email bean = ConfigurationManager.getEmail(I18nUtil
                    .getEmailFilename(I18nUtil.getDefaultLocale(),
                            "etd_collections"));
            bean.addRecipient(email);
            bean.addArgument(strTitle);
            bean.addArgument("" + item.getID());
            bean.addArgument(handleService.findHandle(context, item));
            bean.addArgument(etdcollection.getName());
            bean.send();
        }
    }

    /*********************************************************** reportItem */
    /**
     * Report a successfully loaded item
     */

    private static void reportItem(Context c, Item item, String strHandle,
            Set sCollections) throws Exception
    {

        StringBuffer sb = new StringBuffer();

        sb.append("Item loaded: " + strHandle + "\n");

        // Title
        String title = itemService.getMetadataFirstValue(item, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
        sb.append("  Title: " + title + "\n");

        // Collections
        sb.append("  Collection: " + etdcollection.getName() + "\n");
        for (Iterator ic = sCollections.iterator(); ic.hasNext();)
        {
            Collection coll = (Collection) ic.next();
            sb.append("  Collection: " + coll.getName() + "\n");
        }

        log.info(sb.toString());
    }

    /************************************************************** toString */
    /**
     * Get string representation of xml Document.
     */

    public static String toString(Document doc) throws java.io.IOException
    {
        StringWriter sw = new StringWriter();
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(sw, format);
        writer.write(doc);
        return sw.toString();
    }

}
