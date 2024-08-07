/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.PreviewContentDAO;
import org.dspace.content.service.PreviewContentService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the PreviewContent object.
 *
 * @author Michaela Paurikova (dspace at dataquest.sk)
 */
public class PreviewContentServiceImpl implements PreviewContentService {

    /**
     * logger
     */
    private static final Logger log = LoggerFactory.getLogger(PreviewContentServiceImpl.class);


    @Autowired
    PreviewContentDAO previewContentDAO;
    @Autowired(required = true)
    AuthorizeService authorizeService;

    @Override
    public PreviewContent create(Context context, Bitstream bitstream, String name, String content,
                                 boolean isDirectory, String size, Map<String, PreviewContent> subPreviewContents)
            throws SQLException {
        //no authorization required!
        // Create a table row
        PreviewContent previewContent = previewContentDAO.create(context, new PreviewContent(bitstream, name, content,
                isDirectory, size, subPreviewContents));
        log.info("Created new preview content of ID = {}", previewContent.getID());
        return previewContent;
    }

    @Override
    public PreviewContent create(Context context, PreviewContent previewContent) throws SQLException {
        //no authorization required!
        PreviewContent newPreviewContent = previewContentDAO.create(context, new PreviewContent(previewContent));
        log.info("Created new preview content of ID = {}", newPreviewContent.getID());
        return newPreviewContent;
    }

    @Override
    public void delete(Context context, PreviewContent previewContent) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to delete an CLARIN Content Preview");
        }
        previewContentDAO.delete(context, previewContent);
    }

    @Override
    public PreviewContent find(Context context, int valueId) throws SQLException {
        return previewContentDAO.findByID(context, PreviewContent.class, valueId);
    }

    @Override
    public List<PreviewContent> findByBitstream(Context context, UUID bitstreamId) throws SQLException {
        return previewContentDAO.findByBitstream(context, bitstreamId);
    }

    @Override
    public List<PreviewContent> findRootByBitstream(Context context, UUID bitstreamId) throws SQLException {
        return previewContentDAO.findRootByBitstream(context, bitstreamId);
    }

    @Override
    public List<PreviewContent> findAll(Context context) throws SQLException {
        return previewContentDAO.findAll(context, PreviewContent.class);
    }
}
