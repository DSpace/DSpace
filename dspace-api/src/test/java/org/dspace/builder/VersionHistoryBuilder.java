/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;

/**
 * Builder to construct Version History objects
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class VersionHistoryBuilder extends AbstractBuilder<VersionHistory, VersionHistoryService> {

    private VersionHistory versionHistory;

    protected VersionHistoryBuilder(Context context) {
        super(context);
    }

    public static VersionHistoryBuilder createVersionHistory(final Context context) {
        VersionHistoryBuilder builder = new VersionHistoryBuilder(context);
        return builder.create(context);
    }

    private VersionHistoryBuilder create(final Context context) {
        this.context = context;
        try {
            versionHistory = versionHistoryService.create(context);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            versionHistory = c.reloadEntity(versionHistory);
            delete(c, versionHistory);
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public VersionHistory build() throws SQLException, AuthorizeException {
        try {
            context.dispatchEvents();
            indexingService.commit();
            return versionHistory;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public void delete(Context c, VersionHistory dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    @Override
    protected VersionHistoryService getService() {
        return versionHistoryService;
    }
}
