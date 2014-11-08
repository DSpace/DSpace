/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate.service;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * The Citation Document produces a dissemination package (DIP) that is different that the archival package (AIP).
 * In this case we append the descriptive metadata to the end (configurable) of the document. i.e. last page of PDF.
 * So instead of getting the original PDF, you get a cPDF (with citation information added).
 *
 * @author Peter Dietz (peter@longsight.com)
 */
public interface CitationDocumentService {

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
    public Boolean isCitationEnabledForBitstream(Bitstream bitstream, Context context) throws SQLException;


    public boolean canGenerateCitationVersion(Context context, Bitstream bitstream) throws SQLException;

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
    public File makeCitedDocument(Context context, Bitstream bitstream)
            throws IOException, SQLException, AuthorizeException, COSVisitorException;

    public int drawStringWordWrap(PDPage page, PDPageContentStream contentStream, String text,
                                    int startX, int startY, PDFont pdfFont, float fontSize) throws IOException;

    public String getOwningCommunity(Context context, Item item);

    public String getOwningCollection(Item item);

    public String getAllMetadataSeparated(Item item, String metadataKey);

    /**
     * @param page
     * @param contentStream
     * @param y the y-coordinate of the first row
     * @param margin the padding on left and right of table
     * @param content a 2d array containing the table data
     * @throws IOException
     */
    public void drawTable(PDPage page, PDPageContentStream contentStream,
                                 float y, float margin,
                                 String[][] content, PDFont font, int fontSize, boolean cellBorders) throws IOException;

}
