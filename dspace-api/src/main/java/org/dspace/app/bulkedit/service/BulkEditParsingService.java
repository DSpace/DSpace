/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import org.dspace.app.bulkedit.BulkEditChange;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;

/**
 * Service for parsing bulk-edit changes from a given input stream to a list of {@link BulkEditChange}s
 */
public interface BulkEditParsingService {
    /**
     * Read the input stream and parse it into a list of {@link BulkEditChange}s
     * No actual changes will be persisted throughout the parsing, this happens in {@link BulkEditService} instead
     * @param c     DSpace context
     * @param is    The import source to parse
     */
    List<BulkEditChange> parse(Context c, InputStream is) throws SQLException, MetadataImportException, IOException;

    /**
     * Optionally set a {@link DSpaceRunnableHandler} to log the parsing process
     * @param handler   {@link DSpaceRunnableHandler}
     */
    void setHandler(DSpaceRunnableHandler handler);
}
