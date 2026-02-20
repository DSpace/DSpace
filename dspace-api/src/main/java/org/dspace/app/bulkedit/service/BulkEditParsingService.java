/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.app.bulkedit.BulkEditChange;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;

/**
 * Service for parsing bulk-edit changes from a given source object to a list of {@link BulkEditChange}s
 *
 * Warning: This service and its implementation are stateful, in that a new instance will be created every time it is
 *          requested. This is by design because the service will keep information about multiple related changes until
 *          it is done parsing them all and this ensures none of the information leaks between other calls/processes.
 *          This means the service should never be Autowired and should instead be requested through the
 *          {@link BulkEditServiceFactory} wherever the call is made to parse and/or apply the changes.
 *
 * @param <T> The type of source object containing information about a batch-edit to parse into {@link BulkEditChange}s
 */
public interface BulkEditParsingService<T> {
    /**
     * Read the source object and parse it into a list of {@link BulkEditChange}s
     * No actual changes will persist throughout the parsing, this happens in {@link BulkEditService} instead
     * @param context   DSpace context
     * @param source    Source object to parse
     */
    List<BulkEditChange> parse(Context context, T source)
        throws MetadataImportException, SQLException, AuthorizeException, IOException;

    /**
     * Optionally set a {@link DSpaceRunnableHandler} to log the parsing process
     * @param handler   {@link DSpaceRunnableHandler}
     */
    void setHandler(DSpaceRunnableHandler handler);
}
