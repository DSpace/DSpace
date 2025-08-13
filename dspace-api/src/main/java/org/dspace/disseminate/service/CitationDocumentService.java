/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate.service;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;

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
     * We need to determine if we will intercept this bitstream download, and give out a citation dissemination
     * rendition.
     *
     * What will trigger a redirect/intercept?
     * Citation enabled globally (all citable bitstreams will get "watermarked") modules/disseminate-citation:
     * enable_globally
     * OR
     * The container is this object is "allow list" enabled.
     * - community:  modules/disseminate-citation: enabled_communities
     * - collection: modules/disseminate-citation: enabled_collections
     * AND
     * This User is not an admin. (Admins need to be able to view the "raw" original instead.)
     * AND
     * This object is citation-able (presently, just PDF)
     *
     * The module must be enabled, before the permission level checks happen.
     *
     * @param bitstream DSpace bitstream
     * @param context   DSpace context
     * @return true or false
     * @throws SQLException if error
     */
    Boolean isCitationEnabledForBitstream(Bitstream bitstream, Context context) throws SQLException;

    /**
     * @param context   DSpace Context
     * @param bitstream DSpace Bitstream
     * @return true or false
     * @throws SQLException if error
     */
    boolean canGenerateCitationVersion(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Creates a
     * cited document from the given bitstream of the given item. This
     * requires that bitstream is contained in item.
     * <p>
     * The Process for adding a cover page is as follows:
     * <ol>
     * <li> Load source file into PdfReader and create a
     * Document to put our cover page into.</li>
     * <li> Create cover page and add content to it.</li>
     * <li> Concatenate the coverpage and the source
     * document.</li>
     * </ol>
     *
     * @param context   DSpace context
     * @param bitstream The source bitstream being cited. This must be a PDF.
     * @return The temporary File that is the finished, cited document.
     * @throws IOException        if IO error
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    Pair<byte[], Long> makeCitedDocument(Context context, Bitstream bitstream)
            throws IOException, SQLException, AuthorizeException;

}