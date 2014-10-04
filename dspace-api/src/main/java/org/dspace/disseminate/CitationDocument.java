/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import java.awt.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

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
     * @throws java.io.FileNotFoundException
     * @throws SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    private File makeCitedDocument(Bitstream bitstream, CitationMeta cMeta)
            throws IOException, SQLException, AuthorizeException, COSVisitorException {
        //Read the source bitstream
        PDDocument document = new PDDocument();
        PDDocument sourceDocument = new PDDocument();
        sourceDocument = sourceDocument.load(bitstream.retrieve());
        log.info("loaded pdf");
        PDPage coverPage = new PDPage(PDPage.PAGE_SIZE_LETTER);

        List<PDPage> sourcePageList = sourceDocument.getDocumentCatalog().getAllPages();
        //Is the citation-page the first page or last-page?
        if(isCitationFirstPage()) {
            //citation as cover page
            document.addPage(coverPage);
            for(PDPage sourcePage : sourcePageList) {
                document.addPage(sourcePage);
            }
        } else {
            //citation as tail page
            for(PDPage sourcePage : sourcePageList) {
                document.addPage(sourcePage);
            }
            document.addPage(coverPage);
        }
        log.info("added pages");
        sourcePageList.clear();

        generateCoverPage(document, coverPage, cMeta);
        log.info("3");

        String tempDirString = ConfigurationManager.getProperty("dspace.dir") + "/temp";
        File tempDir = new File(tempDirString);
        if(!tempDir.exists()) {
            boolean success = tempDir.mkdir();
            if(success) {
                log.info("Created temp dir");
            } else {
                log.info("Not created temp dir");
            }
        } else {
            log.info(tempDir + " exists");
        }

        document.save(tempDir.getAbsolutePath() + "/bitstream.cover.pdf");
        document.close();
        sourceDocument.close();
        return new File(tempDir.getAbsolutePath() + "/bitstream.cover.pdf");
    }

    private void generateCoverPage(PDDocument document, PDPage coverPage, CitationMeta citationMeta) throws IOException, COSVisitorException {
        String[] header1 = {"The Ohio State University", ""};
        String[] header2 = {"Knowledge Bank", "http://kb.osu.edu"};
        String[] fields1 = {"dc.date.issued", "dc.date.created"};
        String[] fields2 = {"dc.title", "dc.creator", "dc.contributor.author", "dc.publisher"};
        String[] fields3 = {"dc.identifier.citation", "dc.identifier.uri"};
        String footer = "Downloaded from the Knowledge Bank, The Ohio State University's intitutional repository";

        PDPageContentStream contentStream = new PDPageContentStream(document, coverPage);
        try {
            Item item = citationMeta.getItem();
            int ypos = 700;
            int xpos = 50;
            int ygap = 20;
            log.info("1");

            PDFont fontHelvetica = PDType1Font.HELVETICA;
            PDFont fontHelveticaBold = PDType1Font.HELVETICA_BOLD;
            PDFont fontHelveticaOblique = PDType1Font.HELVETICA_OBLIQUE;

            log.info("2");

            String[][] content = {header1};
            drawTable(coverPage, contentStream, ypos, xpos, content);
            ypos -=(ygap);

            String[][] content2 = {header2};
            drawTable(coverPage, contentStream, ypos, xpos, content2);
            ypos -=ygap;

            contentStream.setNonStrokingColor(Color.BLACK);
            contentStream.fillRect(xpos, ypos, 500, 1);
            contentStream.closeAndStroke();
            //ypos -=(ygap/2);

            String[][] content3 = {{getOwningCommunity(item), getOwningCollection(item)}};
            drawTable(coverPage, contentStream, ypos, xpos, content3);
            ypos -=ygap;

            contentStream.setNonStrokingColor(Color.BLACK);
            contentStream.fillRect(xpos, ypos, 500, 1);
            contentStream.closeAndStroke();
            ypos -=(ygap*2);

            log.info("Drew table");

            for(String field : fields1) {
                PDFont font = fontHelvetica;
                int fontSize = 12;

                if(field.contains("title")) {
                    font = fontHelveticaBold;
                    fontSize = 26;
                } else if(field.contains("identifier")) {
                    fontSize = 11;
                }

                if(StringUtils.isNotEmpty(item.getMetadata(field))) {
                    ypos = drawStringWordWrap(coverPage, contentStream, item.getMetadata(field), xpos, ypos, font, fontSize);
                }
            }

            contentStream.setNonStrokingColor(Color.BLACK);
            contentStream.fillRect(xpos, ypos, 500, 1);
            contentStream.closeAndStroke();
            ypos -=(ygap*2);

            for(String field : fields2) {
                PDFont font = fontHelvetica;
                int fontSize = 12;
                if(field.contains("title")) {
                    font = fontHelveticaBold;
                    fontSize = 26;
                } else if(field.contains("identifier")) {
                    fontSize = 11;
                }

                if(StringUtils.isNotEmpty(item.getMetadata(field))) {
                    ypos = drawStringWordWrap(coverPage, contentStream, item.getMetadata(field), xpos, ypos, font, fontSize);
                }
            }

            contentStream.setNonStrokingColor(Color.BLACK);
            contentStream.fillRect(xpos, ypos, 500, 1);
            contentStream.closeAndStroke();
            ypos -=(ygap*2);

            for(String field : fields3) {
                PDFont font = fontHelvetica;
                int fontSize = 12;
                if(field.contains("title")) {
                    font = fontHelveticaBold;
                    fontSize = 26;
                } else if(field.contains("identifier")) {
                    fontSize = 11;
                }

                if(StringUtils.isNotEmpty(item.getMetadata(field))) {
                    ypos = drawStringWordWrap(coverPage, contentStream, item.getMetadata(field), xpos, ypos, font, fontSize);
                }
            }



            contentStream.beginText();
            contentStream.setFont(fontHelveticaOblique, 11);
            contentStream.moveTextPositionByAmount(xpos, ypos);
            contentStream.drawString(footer);
            contentStream.endText();
            log.info("13");
            ypos -=ygap;

        } finally {
            contentStream.close();
        }

        log.info("14");
    }

    public int drawStringWordWrap(PDPage page, PDPageContentStream contentStream, String text,
                                    int startX, int startY, PDFont pdfFont, float fontSize) throws IOException {
        //PDFont pdfFont = PDType1Font.HELVETICA;
        //float fontSize = 25;
        float leading = 1.5f * fontSize;


        PDRectangle mediabox = page.findMediaBox();
        float margin = 72;
        float width = mediabox.getWidth() - 2*margin;
        //float startX = mediabox.getLowerLeftX() + margin;
        //float startY = mediabox.getUpperRightY() - margin;

        //String text = "I am trying to create a PDF file with a lot of text contents in the document. I am using PDFBox";
        List<String> lines = new ArrayList<String>();
        int lastSpace = -1;
        while (text.length() > 0)
        {
            int spaceIndex = text.indexOf(' ', lastSpace + 1);
            if (spaceIndex < 0)
            {
                lines.add(text);
                text = "";
            }
            else
            {
                String subString = text.substring(0, spaceIndex);
                float size = fontSize * pdfFont.getStringWidth(subString) / 1000;
                if (size > width)
                {
                    if (lastSpace < 0) // So we have a word longer than the line... draw it anyways
                        lastSpace = spaceIndex;
                    subString = text.substring(0, lastSpace);
                    lines.add(subString);
                    text = text.substring(lastSpace).trim();
                    lastSpace = -1;
                }
                else
                {
                    lastSpace = spaceIndex;
                }
            }
        }

        contentStream.beginText();
        contentStream.setFont(pdfFont, fontSize);
        contentStream.moveTextPositionByAmount(startX, startY);
        int currentY = startY;
        for (String line: lines)
        {
            contentStream.drawString(line);
            currentY -= leading;
            contentStream.moveTextPositionByAmount(0, -leading);
        }
        contentStream.endText();
        return currentY;
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

    public String getAllMetadataSeperated(Item item, String metadataKey) {
        DCValue[] dcValues = item.getMetadataByMetadataString(metadataKey);

        ArrayList<String> valueArray = new ArrayList<String>();

        for(DCValue dcValue : dcValues) {
            valueArray.add(dcValue.value);
        }

        return StringUtils.join(valueArray.toArray(), "; ");
    }

    /**
     * @param page
     * @param contentStream
     * @param y the y-coordinate of the first row
     * @param margin the padding on left and right of table
     * @param content a 2d array containing the table data
     * @throws IOException
     */
    public static void drawTable(PDPage page, PDPageContentStream contentStream,
                                 float y, float margin,
                                 String[][] content) throws IOException {
        final int rows = content.length;
        final int cols = content[0].length;
        final float rowHeight = 20f;
        final float tableWidth = page.findMediaBox().getWidth()-(2*margin);
        final float tableHeight = rowHeight * rows;
        final float colWidth = tableWidth/(float)cols;
        final float cellMargin=5f;

        //draw the rows
        //float nexty = y ;
        //for (int i = 0; i <= rows; i++) {
        //    contentStream.drawLine(margin,nexty,margin+tableWidth,nexty);
        //    nexty-= rowHeight;
        //}

        //draw the columns
        //float nextx = margin;
        //for (int i = 0; i <= cols; i++) {
        //    contentStream.drawLine(nextx,y,nextx,y-tableHeight);
        //    nextx += colWidth;
        //}

        //now add the text
        contentStream.setFont(PDType1Font.HELVETICA_BOLD,12);

        float textx = margin+cellMargin;
        float texty = y-15;
        for(int i = 0; i < content.length; i++){
            for(int j = 0 ; j < content[i].length; j++){
                String text = content[i][j];
                contentStream.beginText();
                contentStream.moveTextPositionByAmount(textx,texty);
                contentStream.drawString(text);
                contentStream.endText();
                textx += colWidth;
            }
            texty-=rowHeight;
            textx = margin+cellMargin;
        }
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
