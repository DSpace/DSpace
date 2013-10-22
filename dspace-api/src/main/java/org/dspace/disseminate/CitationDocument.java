package org.dspace.disseminate;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.*;

/**
 * The Citation Document produces a dissemination package (DIP) that is different that the archival package (AIP).
 * In this case we append the descriptive metadata to the end (configurable) of the document. i.e. last page of PDF.
 * So instead of getting the original PDF, you get a cPDF (with citation information added).
 *
 * @author Peter Dietz (dietz.72@osu.edu)
 */
public class CitationDocument {
    /**
     * Class Logger
     */
    private static Logger log = Logger.getLogger(CitationDocument.class);

    /**
     * A set of MIME types that can have a citation page added to them. That is,
     * MIME types in this set can be converted to a PDF which is then prepended
     * with a citation page.
     */
    private static final Set<String> VALID_TYPES = new HashSet<String>(2);

    /**
     * A set of MIME types that refer to a PDF
     */
    private static final Set<String> PDF_MIMES = new HashSet<String>(2);

    /**
     * A set of MIME types that refer to a JPEG, PNG, or GIF
     */
    private static final Set<String> RASTER_MIMES = new HashSet<String>();
    /**
     * A set of MIME types that refer to a SVG
     */
    private static final Set<String> SVG_MIMES = new HashSet<String>();

    /**
     * Comma separated list of collections handles to enable citation for.
     * webui.citation.enabled_collections, default empty/none. ex: =1811/123, 1811/345
     */
    private static String citationEnabledCollections = null;

    /**
     * Comma separated list of community handles to enable citation for.
     * webui.citation.enabled_communties, default empty/none. ex: =1811/123, 1811/345
     */
    private static String citationEnabledCommunities = null;

    /**
     * List of all enabled collections, inherited/determined for those under communities.
     */
    private static ArrayList<String> citationEnabledCollectionsList;


    static {
        // Add valid format MIME types to set. This could be put in the Schema
        // instead.
        //Populate RASTER_MIMES
        SVG_MIMES.add("image/jpeg");
        SVG_MIMES.add("image/pjpeg");
        SVG_MIMES.add("image/png");
        SVG_MIMES.add("image/gif");
        //Populate SVG_MIMES
        SVG_MIMES.add("image/svg");
        SVG_MIMES.add("image/svg+xml");


        //Populate PDF_MIMES
        PDF_MIMES.add("application/pdf");
        PDF_MIMES.add("application/x-pdf");

        //Populate VALID_TYPES
        VALID_TYPES.addAll(PDF_MIMES);


        //Load enabled collections
        citationEnabledCollections = ConfigurationManager.getProperty("disseminate-citation", "enabled_collections");
        citationEnabledCollectionsList = new ArrayList<String>();
        if(citationEnabledCollections != null && citationEnabledCollections.length() > 0) {
            String[] collectionChunks = citationEnabledCollections.split(",");
            for(String collectionString : collectionChunks) {
                citationEnabledCollectionsList.add(collectionString.trim());
            }

        }

        //Load enabled communities, and add to collection-list
        citationEnabledCommunities = ConfigurationManager.getProperty("disseminate-citation", "enabled_communities");
        if(citationEnabledCollectionsList == null) {
            citationEnabledCollectionsList = new ArrayList<String>();
        }

        if(citationEnabledCommunities != null && citationEnabledCommunities.length() > 0) {
            try {
                String[] communityChunks = citationEnabledCommunities.split(",");
                for(String communityString : communityChunks) {
                    Context context = new Context();
                    DSpaceObject dsoCommunity = HandleManager.resolveToObject(context, communityString.trim());
                    if(dsoCommunity instanceof Community) {
                        Community community = (Community)dsoCommunity;
                        Collection[] collections = community.getAllCollections();

                        for(Collection collection : collections) {
                            citationEnabledCollectionsList.add(collection.getHandle());
                        }
                    } else {
                        log.error("Invalid community for citation.enabled_communities, value:" + communityString.trim());
                    }

                }
            } catch (SQLException e) {
                log.error(e.getMessage());
            }

        }
    }


    public CitationDocument() {
    }

    /**
     * Boolean to determine is citation-functionality is enabled globally for entire site.
     * config/module/disseminate-citation: enable_globally, default false. true=on, false=off
     */
    private static Boolean citationEnabledGlobally = null;

    private static boolean isCitationEnabledGlobally() {
        if(citationEnabledGlobally == null) {
            citationEnabledGlobally = ConfigurationManager.getBooleanProperty("disseminate-citation", "enable_globally", false);
        }

        return citationEnabledGlobally;
    }




    private static boolean isCitationEnabledThroughCollection(Bitstream bitstream) throws SQLException {
        //TODO Should we re-check configs, and set the collections list?

        //Reject quickly if no-enabled collections
        if(citationEnabledCollectionsList.size() == 0) {
            return false;
        }

        DSpaceObject owningDSO = bitstream.getParentObject();
        if(owningDSO instanceof Item) {
            Item item = (Item)owningDSO;

            Collection[] collections = item.getCollections();

            for(Collection collection : collections) {
                if(citationEnabledCollectionsList.contains(collection.getHandle())) {
                    return true;
                }
            }
        }

        // If previous logic didn't return true, then we're false
        return false;
    }




    /**
     * Repository policy can specify to have a custom citation cover/tail page to the document, which embeds metadata.
     * We need to determine if we will intercept this bitstream download, and give out a citation dissemination rendition.
     *
     * What will trigger a redirect/intercept?
     *  Citation enabled globally (all citable bitstreams will get "watermarked") modules/disseminate-citation: enable_globally
     *    OR
     *  The container is this object is whitelist enabled.
     *      - community:  modules/disseminate-citation: enabled_communities
     *      - collection: modules/disseminate-citation: enabled_collections
     * AND
     *  This User is not an admin. (Admins need to be able to view the "raw" original instead.)
     * AND
     *  This object is citation-able (presently, just PDF)
     *
     *  The module must be enabled, before the permission level checks happen.
     * @param bitstream
     * @return
     */
    public static Boolean isCitationEnabledForBitstream(Bitstream bitstream, Context context) throws SQLException {
        if(isCitationEnabledGlobally() || isCitationEnabledThroughCollection(bitstream)) {

            boolean adminUser = AuthorizeManager.isAdmin(context);

            if(!adminUser && canGenerateCitationVersion(bitstream)) {
                return true;
            }

        }

        // If previous logic didn't return true, then we're false.
        return false;
    }

    /**
     * Should the citation page be the first page of the document, or the last page?
     * default => true. true => first page, false => last page
     * citation_as_first_page=true
     */
    private static Boolean citationAsFirstPage = null;

    private static Boolean isCitationFirstPage() {
        if(citationAsFirstPage == null) {
            citationAsFirstPage = ConfigurationManager.getBooleanProperty("disseminate-citation", "citation_as_first_page", true);
        }

        return citationAsFirstPage;
    }
    
    public static boolean canGenerateCitationVersion(Bitstream bitstream) {
        return VALID_TYPES.contains(bitstream.getFormat().getMIMEType());        
    }
    
    public File makeCitedDocument(Bitstream bitstream) {
        try {
        
            Item item = (Item) bitstream.getParentObject();
            CitationMeta cm = new CitationMeta(item);
            if(cm == null) {
                log.error("CitationMeta was null");
            }
            
            File citedDocumentFile = makeCitedDocument(bitstream, cm);
            if(citedDocumentFile == null) {
                log.error("Got a null citedDocumentFile in makeCitedDocument for bitstream");
            }
            return citedDocumentFile;
        } catch (Exception e) {
            log.error("makeCitedDocument from Bitstream fail!" + e.getMessage());
            return null;
        }
        
    }

    /**
     * Creates a
     * cited document from the given bitstream of the given item. This
     * requires that bitstream is contained in item.
     * <p>
     * The Process for adding a cover page is as follows:
     * <ol>
     *  <li> Load source file into PdfReader and create a
     *     Document to put our cover page into.</li>
     *  <li> Create cover page and add content to it.</li>
     *  <li> Concatenate the coverpage and the source
     *     document.</li>
     * </p>
     *
     * @param bitstream The source bitstream being cited. This must be a PDF.
     * @param cMeta The citation information used to generate the coverpage.
     * @return The temporary File that is the finished, cited document.
     * @throws com.itextpdf.text.DocumentException
     * @throws java.io.FileNotFoundException
     * @throws SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    private File makeCitedDocument(Bitstream bitstream, CitationMeta cMeta)
            throws DocumentException, IOException, SQLException, AuthorizeException {
        //Read the source bitstream
        PdfReader source = new PdfReader(bitstream.retrieve());

        Document citedDoc = new Document(PageSize.LETTER);

        File coverTemp = File.createTempFile(bitstream.getName(), ".cover.pdf");

        //Need a writer instance to make changed to the document.
        PdfWriter writer = PdfWriter.getInstance(citedDoc, new FileOutputStream(coverTemp));

        //Call helper function to add content to the coverpage.
        this.generateCoverPage(citedDoc, writer, cMeta);

        //Create reader from finished cover page.
        PdfReader cover = new PdfReader(new FileInputStream(coverTemp));

        //Get page labels from source document
        String[] labels = PdfPageLabels.getPageLabels(source);

        //Concatenate the finished cover page with the source document.
        File citedTemp = File.createTempFile(bitstream.getName(), ".cited.pdf");
        OutputStream citedOut = new FileOutputStream(citedTemp);
        PdfConcatenate concat = new PdfConcatenate(citedOut);
        concat.open();

        //Is the citation-page the first page or last-page?
        if(isCitationFirstPage()) {
            //citation as cover page
            concat.addPages(cover);
            concat.addPages(source);
        } else {
            //citation as tail page
            concat.addPages(source);
            concat.addPages(cover);
        }

        //Put all of our labels in from the orignal document.
        if (labels != null) {
            PdfPageLabels citedPageLabels = new PdfPageLabels();
            log.debug("Printing arbitrary page labels.");

            for (int i = 0; i < labels.length; i++) {
                citedPageLabels.addPageLabel(i + 1, PdfPageLabels.EMPTY, labels[i]);
                log.debug("Label for page: " + (i + 1) + " -> " + labels[i]);
            }
            citedPageLabels.addPageLabel(labels.length + 1, PdfPageLabels.EMPTY, "Citation Page");
            concat.getWriter().setPageLabels(citedPageLabels);
        }

        //Close it up
        concat.close();

        //Close the cover-page
        writer.close();
        coverTemp.delete();

        citedTemp.deleteOnExit();
        return citedTemp;
    }

    /**
     * Takes a DSpace {@link Bitstream} and uses its associated METADATA to
     * create a cover page.
     *
     * @param cDoc The cover page document to add cited information to.
     * @param writer
     * @param cMeta
     *            METADATA retrieved from the parent collection.
     * @throws IOException
     * @throws DocumentException
     */
    private void generateCoverPage(Document cDoc, PdfWriter writer, CitationMeta cMeta) throws DocumentException {
        cDoc.open();
        writer.setCompressionLevel(0);

        Item item = cMeta.getItem();

        //Set up some fonts
        Font helv26 =           FontFactory.getFont(FontFactory.HELVETICA,          26f,    BaseColor.BLACK);
        Font helv16 =           FontFactory.getFont(FontFactory.HELVETICA,          16f,    BaseColor.BLACK);
        Font helv12 =           FontFactory.getFont(FontFactory.HELVETICA,          12f,    BaseColor.BLACK);
        Font helv12_italic =    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,  12f,    BaseColor.BLACK);
        Font helv11_bold =      FontFactory.getFont(FontFactory.HELVETICA_BOLD,     11f,    BaseColor.BLACK);
        Font helv9 =            FontFactory.getFont(FontFactory.HELVETICA,          9f,     BaseColor.BLACK);

        // 1 - Header:
        //  University Name
        //  Repository Name                                                        repository.url
        Paragraph university = new Paragraph("The Ohio State University", helv11_bold);
        cDoc.add(university);

        PdfPTable repositoryTable = new PdfPTable(2);
        repositoryTable.setWidthPercentage(100);

        Chunk repositoryName =  new Chunk("Knowledge Bank", helv11_bold);
        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorderWidth(0);
        nameCell.addElement(repositoryName);

        Chunk repositoryURL =   new Chunk("kb.osu.edu", helv11_bold);
        repositoryURL.setAnchor("http://kb.osu.edu");

        PdfPCell urlCell = new PdfPCell();
        urlCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        urlCell.setBorderWidth(0);
        urlCell.addElement(repositoryURL);

        repositoryTable.addCell(nameCell);
        repositoryTable.addCell(urlCell);

        repositoryTable.setSpacingAfter(5);

        cDoc.add(repositoryTable);

        // Line Separator
        LineSeparator lineSeparator = new LineSeparator();
        cDoc.add(lineSeparator);

        // 2 - Bread Crumbs
        // Community Name                                                          Collection Name
        PdfPTable breadcrumbTable = new PdfPTable(2);
        breadcrumbTable.setWidthPercentage(100);

        Chunk communityName =  new Chunk(getOwningCommunity(item), helv9);
        PdfPCell commCell = new PdfPCell();
        commCell.setBorderWidth(0);
        commCell.addElement(communityName);

        Chunk collectionName =   new Chunk(getOwningCollection(item), helv9);
        PdfPCell collCell = new PdfPCell();
        collCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        collCell.setBorderWidth(0);
        collCell.addElement(collectionName);

        breadcrumbTable.addCell(commCell);
        breadcrumbTable.addCell(collCell);

        breadcrumbTable.setSpacingBefore(5);
        breadcrumbTable.setSpacingAfter(5);

        cDoc.add(breadcrumbTable);

        // Line Separator
        cDoc.add(lineSeparator);

        // 3 - Metadata
        // date.issued
        // dc.title
        // dc.creator; dc.creator
        Paragraph dateIssued = new Paragraph(getFirstMetadata(item, "dc.date.issued"), helv12);
        dateIssued.setSpacingBefore(20);
        cDoc.add(dateIssued);

        Paragraph title = new Paragraph(item.getName(), helv26);
        title.setSpacingBefore(15);
        cDoc.add(title);

        Paragraph creators = new Paragraph(getAllMetadataSeperated(item, "dc.creator"), helv16);
        creators.setSpacingBefore(30);
        creators.setSpacingAfter(20);
        cDoc.add(creators);

        // Line Separator
        cDoc.add(lineSeparator);

        // 4 - Citation
        // dc.identifier.citation
        // dc.identifier.uri
        Paragraph citation      = new Paragraph(getFirstMetadata(item, "dc.identifier.citation"), helv12);

        Chunk identifierChunk = new Chunk(getFirstMetadata(item, "dc.identifier.uri"), helv12);
        identifierChunk.setAnchor(getFirstMetadata(item, "dc.identifier.uri"));

        Paragraph identifier    = new Paragraph();
        identifier.add(identifierChunk);


        cDoc.add(citation);
        cDoc.add(identifier);

        // 5 - License
        // Downloaded from the Knowledge Bank, The Ohio State University's institutional repository
        Paragraph license = new Paragraph("Downloaded from the Knowledge Bank, The Ohio State University's institutional repository", helv12_italic);
        license.setSpacingBefore(10);
        cDoc.add(license);

        cDoc.close();
    }

    /**
     * Attempts to add a Logo to the document from the given resource. Returns
     * true on success and false on failure.
     *
     * @param doc The document to add the logo to. (Added to the top right
     * corner of the first page.
     * @param writer The writer associated with the given Document.
     * @param res The resource/path to the logo file. This file can be any of
     * the following formats:
     *  GIF, PNG, JPEG, PDF
     *
     * @return Succesfully added logo to document.
     */
    private boolean addLogoToDocument(Document doc, PdfWriter writer, String res) {
        boolean ret = false;
        try {
            //First we try to get the logo as if it is a Java Resource
            URL logoURL = this.getClass().getResource(res);
            log.debug(res + " -> " + logoURL.toString());
            if (logoURL == null) {
                logoURL = new URL(res);
            }

            if (logoURL != null) {
                String mtype = URLConnection.guessContentTypeFromStream(logoURL.openStream());
                if (mtype == null) {
                    mtype = URLConnection.guessContentTypeFromName(res);
                }
                log.debug("Determined MIMETYPE of logo: " + mtype);
                if (PDF_MIMES.contains(mtype)) {
                    //Handle pdf logos.
                    PdfReader reader = new PdfReader(logoURL);
                    PdfImportedPage logoPage = writer.getImportedPage(reader, 1);
                    Image logo = Image.getInstance(logoPage);
                    float x = doc.getPageSize().getWidth() - doc.rightMargin() - logo.getScaledWidth();
                    float y = doc.getPageSize().getHeight() - doc.topMargin() - logo.getScaledHeight();
                    logo.setAbsolutePosition(x, y);
                    doc.add(logo);
                    ret = true;
                } else if (RASTER_MIMES.contains(mtype)) {
                    //Use iText's Image class
                    Image logo = Image.getInstance(logoURL);

                    //Determine the position of the logo (upper-right corner) and
                    //place it there.
                    float x = doc.getPageSize().getWidth() - doc.rightMargin() - logo.getScaledWidth();
                    float y = doc.getPageSize().getHeight() - doc.topMargin() - logo.getScaledHeight();
                    logo.setAbsolutePosition(x, y);
                    writer.getDirectContent().addImage(logo);
                    ret = true;
                } else if (SVG_MIMES.contains(mtype)) {
                    //Handle SVG Logos
                    log.error("SVG Logos are not supported yet.");
                } else {
                    //Cannot use other mimetypes
                    log.debug("Logo MIMETYPE is not supported.");
                }
            } else {
                log.debug("Could not create URL to Logo resource: " + res);
            }
        } catch (Exception e) {
            log.error("Could not add logo (" + res + ") to cited document: "
                    + e.getMessage());
            ret = false;
        }
        return ret;
    }

    public String getOwningCommunity(Item item) {
        try {
            Community[] comms = item.getCommunities();
            if(comms.length > 0) {
                return comms[0].getName();
            } else {
                return " ";
            }

        } catch (SQLException e) {
            log.error(e.getMessage());
            return e.getMessage();
        }
    }

    public String getOwningCollection(Item item) {
        try {
            return item.getOwningCollection().getName();
        } catch (SQLException e) {
            log.error(e.getMessage());
            return e.getMessage();
        }
    }

    public String getFirstMetadata(Item item, String metadataKey) {
        DCValue[] dcValues = item.getMetadata(metadataKey);
        if(dcValues != null && dcValues.length > 0) {
            return dcValues[0].value;
        } else {
            return " ";
        }
    }

    public String getAllMetadataSeperated(Item item, String metadataKey) {
        DCValue[] dcValues = item.getMetadata(metadataKey);
        ArrayList<String> valueArray = new ArrayList<String>();

        for(DCValue dcValue : dcValues) {
            valueArray.add(dcValue.value);
        }

        return StringUtils.join(valueArray.toArray(), "; ");
    }

    /**
     * This wraps the item used in its constructor to make it easier to access
     * METADATA.
     */
    private class CitationMeta {
        private Collection parent;
        private Map<String, String> metaData;
        private Item myItem;

        /**
         * Constructs CitationMeta object from an Item. It uses item specific
         * METADATA as well as METADATA from the owning collection.
         *
         * @param item An Item to get METADATA from.
         * @throws java.sql.SQLException
         */
        public CitationMeta(Item item) throws SQLException {
            this.myItem = item;
            this.metaData = new HashMap<String, String>();
            //Get all METADATA from our this.myItem
            DCValue[] dcvs = this.myItem.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            //Put METADATA in a Map for easy access.
            for (DCValue dsv : dcvs) {
                String[] dsvParts = {dsv.schema, dsv.element, dsv.qualifier, dsv.language, dsv.authority};
                StringBuilder keyBuilder = new StringBuilder();
                for (String part : dsvParts) {
                    if (part != null && part != "") {
                        keyBuilder.append(part + '.');
                    }
                }
                //Remove the trailing '.'
                keyBuilder.deleteCharAt(keyBuilder.length() - 1);
                this.metaData.put(keyBuilder.toString(), dsv.value);
            }

            
            //Get METADATA from the owning Collection
            this.parent = this.myItem.getOwningCollection();
        }

        /**
         * Returns a map of the METADATA for the item associated with this
         * instance of CitationMeta.
         *
         * @return a Map of the METADATA for the associated item.
         */
        public Map<String, String> getMetaData() {
            return this.metaData;
        }

        public Item getItem() {
            return this.myItem;
        }

        public Collection getCollection() {
            return this.parent;
        }

        /**
         * {@inheritDoc}
         * @see Object#toString()
         * @return A string with the format:
         *  CitationPage.CitationMeta {
         *      CONTENT
         *  }
         *  Where CONTENT is the METADATA derived by this class.
         */
        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder(CitationMeta.class.getName());
            ret.append(" {<br />\n\t");
            ret.append(this.parent.getName());
            ret.append("\n\t");
            ret.append(this.myItem.getName());
            ret.append("\n\t");
            ret.append(this.metaData);
            ret.append("\n}\n");
            return ret.toString();
        }
    }

}
