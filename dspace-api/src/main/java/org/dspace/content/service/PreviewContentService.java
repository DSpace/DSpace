/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.PreviewContent;
import org.dspace.core.Context;

/**
 * Service interface class for the PreviewContent object.
 *
 * @author Michaela Paurikova (dspace at dataquest.sk)
 */
public interface PreviewContentService {

    /**
     * Create a new preview content in the database.
     *
     * @param context DSpace context
     * @param bitstream             The bitstream to create a preview content for
     * @param name                  The name of preview content
     * @param content               The content of preview content
     * @param isDirectory           True if preview content is directory else false
     * @param size                  The size of preview content
     * @param subPreviewContents    The sub preview contents of preview content
     * @return The newly created preview content
     * @throws SQLException         If a database error occurs
     */
    PreviewContent create(Context context, Bitstream bitstream, String name, String content,
                          boolean isDirectory, String size, Map<String, PreviewContent> subPreviewContents)
            throws SQLException;

    /**
     * Create a new preview content in the database.
     *
     * @param context           DSpace context
     * @param previewContent    The preview content
     * @return The newly created preview content
     * @throws SQLException     If a database error occurs
     */
    PreviewContent create(Context context, PreviewContent previewContent) throws SQLException;

    /**
     * Delete a preview content from the database.
     *
     * @param context             DSpace context
     * @param previewContent      Deleted preview content
     * @throws SQLException       If a database error occurs
     * @throws AuthorizeException If a user is not authorized
     */
    void delete(Context context, PreviewContent previewContent) throws SQLException, AuthorizeException;

    /**
     * Find preview content based on ID.
     *
     * @param context           DSpace context
     * @param valueId           The ID of the preview content to search for
     * @throws SQLException     If a database error occurs
     */
    PreviewContent find(Context context, int valueId) throws SQLException;

    /**
     * Find all preview content based on bitstream.
     *
     * @param context        DSpace context
     * @param bitstream_id   The ID of the bitstream
     * @throws SQLException  If a database error occurs
     */
    List<PreviewContent> findByBitstream(Context context, UUID bitstream_id) throws SQLException;

    /**
     * Find all preview content based on bitstream that are the root directory.
     *
     * @param context        DSpace context
     * @param bitstream_id   The ID of the bitstream
     * @throws SQLException  If a database error occurs
     */
    List<PreviewContent> findRootByBitstream(Context context, UUID bitstream_id) throws SQLException;

    /**
     * Find all preview contents from database.
     *
     * @param context        DSpace context
     * @throws SQLException  If a database error occurs
     */
    List<PreviewContent> findAll(Context context) throws SQLException;
}
