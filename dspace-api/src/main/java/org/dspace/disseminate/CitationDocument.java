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
 * @author Peter Dietz (peter@longsight.com)
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

    private static File tempDir;

    private static String[] header1;
    private static String[] header2;
    private static String[] fields;
    private static String footer;


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

        // Configurable text/fields, we'll set sane defaults
        String header1Config = ConfigurationManager.getProperty("disseminate-citation", "header1");
        if(StringUtils.isNotBlank(header1Config)) {
            header1 = header1Config.split(",");
        } else {
            header1 = new String[]{"DSpace Institution", ""};
        }

        String header2Config = ConfigurationManager.getProperty("disseminate-citation", "header2");
        if(StringUtils.isNotBlank(header2Config)) {
            header2 = header2Config.split(",");
        } else {
            header2 = new String[]{"DSpace Repository", "http://dspace.org"};
        }

        String fieldsConfig = ConfigurationManager.getProperty("disseminate-citation", "fields");
        if(StringUtils.isNotBlank(fieldsConfig)) {
            fields = fieldsConfig.split(",");
        } else {
            fields = new String[]{"dc.date.issued", "dc.title", "dc.creator", "dc.contributor.author", "dc.publisher", "_line_", "dc.identifier.citation", "dc.identifier.uri"};
        }

        String footerConfig = ConfigurationManager.getProperty("disseminate-citation", "footer");
        if(StringUtils.isNotBlank(footerConfig)) {
            footer = footerConfig;
        } else {
            footer = "Downloaded from DSpace Repository, DSpace Institution's institutional repository";
        }

        //Ensure a temp directory is available
        String tempDirString = ConfigurationManager.getProperty("dspace.dir") + "/temp";
        tempDir = new File(tempDirString);
        if(!tempDir.exists()) {
            boolean success = tempDir.mkdir();
            if(success) {
                log.info("Created temp directory at: " + tempDirString);
            } else {
                log.info("Unable to create temp directory at: " + tempDirString);
            }
        }
    }


    public CitationDocument() {}

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
     * @return The temporary File that is the finished, cited document.
     * @throws java.io.FileNotFoundException
     * @throws SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public File makeCitedDocument(Bitstream bitstream)
            throws IOException, SQLException, AuthorizeException, COSVisitorException {
        PDDocument document = new PDDocument();
        PDDocument sourceDocument = new PDDocument();
        try {
            Item item = (Item) bitstream.getParentObject();
            sourceDocument = sourceDocument.load(bitstream.retrieve());
            PDPage coverPage = new PDPage(PDPage.PAGE_SIZE_LETTER);
            generateCoverPage(document, coverPage, item);
            addCoverPageToDocument(document, sourceDocument, coverPage);

            document.save(tempDir.getAbsolutePath() + "/bitstream.cover.pdf");
            return new File(tempDir.getAbsolutePath() + "/bitstream.cover.pdf");
        } finally {
            sourceDocument.close();
            document.close();
        }
    }

    private void generateCoverPage(PDDocument document, PDPage coverPage, Item item) throws IOException, COSVisitorException {
        PDPageContentStream contentStream = new PDPageContentStream(document, coverPage);
        try {
            int ypos = 760;
            int xpos = 30;
            int xwidth = 550;
            int ygap = 20;

            PDFont fontHelvetica = PDType1Font.HELVETICA;
            PDFont fontHelveticaBold = PDType1Font.HELVETICA_BOLD;
            PDFont fontHelveticaOblique = PDType1Font.HELVETICA_OBLIQUE;
            contentStream.setNonStrokingColor(Color.BLACK);

            String[][] content = {header1};
            drawTable(coverPage, contentStream, ypos, xpos, content, fontHelveticaBold, 11, false);
            ypos -=(ygap);

            String[][] content2 = {header2};
            drawTable(coverPage, contentStream, ypos, xpos, content2, fontHelveticaBold, 11, false);
            ypos -=ygap;

            contentStream.fillRect(xpos, ypos, xwidth, 1);
            contentStream.closeAndStroke();

            String[][] content3 = {{getOwningCommunity(item), getOwningCollection(item)}};
            drawTable(coverPage, contentStream, ypos, xpos, content3, fontHelvetica, 9, false);
            ypos -=ygap;

            contentStream.fillRect(xpos, ypos, xwidth, 1);
            contentStream.closeAndStroke();
            ypos -=(ygap*2);

            for(String field : fields) {
                field = field.trim();
                PDFont font = fontHelvetica;
                int fontSize = 11;
                if(field.contains("title")) {
                    fontSize = 26;
                    ypos -= ygap;
                } else if(field.contains("creator") || field.contains("contributor")) {
                    fontSize = 16;
                }

                if(field.equals("_line_")) {
                    contentStream.fillRect(xpos, ypos, xwidth, 1);
                    contentStream.closeAndStroke();
                    ypos -=(ygap);

                } else if(StringUtils.isNotEmpty(item.getMetadata(field))) {
                    ypos = drawStringWordWrap(coverPage, contentStream, item.getMetadata(field), xpos, ypos, font, fontSize);
                }

                if(field.contains("title")) {
                    ypos -=ygap;
                }
            }

            contentStream.beginText();
            contentStream.setFont(fontHelveticaOblique, 11);
            contentStream.moveTextPositionByAmount(xpos, ypos);
            contentStream.drawString(footer);
            contentStream.endText();
        } finally {
            contentStream.close();
        }
    }

    private void addCoverPageToDocument(PDDocument document, PDDocument sourceDocument, PDPage coverPage) {
        List<PDPage> sourcePageList = sourceDocument.getDocumentCatalog().getAllPages();

        if (isCitationFirstPage()) {
            //citation as cover page
            document.addPage(coverPage);
            for (PDPage sourcePage : sourcePageList) {
                document.addPage(sourcePage);
            }
        } else {
            //citation as tail page
            for (PDPage sourcePage : sourcePageList) {
                document.addPage(sourcePage);
            }
            document.addPage(coverPage);
        }
        sourcePageList.clear();
    }

    public int drawStringWordWrap(PDPage page, PDPageContentStream contentStream, String text,
                                    int startX, int startY, PDFont pdfFont, float fontSize) throws IOException {
        float leading = 1.5f * fontSize;

        PDRectangle mediabox = page.findMediaBox();
        float margin = 72;
        float width = mediabox.getWidth() - 2*margin;

        List<String> lines = new ArrayList<>();
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

    public String getAllMetadataSeparated(Item item, String metadataKey) {
        Metadatum[] dcValues = item.getMetadataByMetadataString(metadataKey);

        ArrayList<String> valueArray = new ArrayList<String>();

        for(Metadatum dcValue : dcValues) {
            if(StringUtils.isNotBlank(dcValue.value)) {
                valueArray.add(dcValue.value);
            }
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
                                 String[][] content, PDFont font, int fontSize, boolean cellBorders) throws IOException {
        final int rows = content.length;
        final int cols = content[0].length;
        final float rowHeight = 20f;
        final float tableWidth = page.findMediaBox().getWidth()-(2*margin);
        final float tableHeight = rowHeight * rows;
        final float colWidth = tableWidth/(float)cols;
        final float cellMargin=5f;

        if(cellBorders) {
            //draw the rows
            float nexty = y ;
            for (int i = 0; i <= rows; i++) {
                contentStream.drawLine(margin,nexty,margin+tableWidth,nexty);
                nexty-= rowHeight;
            }

            //draw the columns
            float nextx = margin;
            for (int i = 0; i <= cols; i++) {
                contentStream.drawLine(nextx,y,nextx,y-tableHeight);
                nextx += colWidth;
            }
        }

        //now add the text
        contentStream.setFont(font, fontSize);

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
}
