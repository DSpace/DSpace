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
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.handle.Handle;
import org.dspace.handle.service.HandleClarinService;

/**
 * Builder to construct Handle objects
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class HandleBuilder extends AbstractBuilder<Handle, HandleClarinService> {

    HandleClarinService handleClarinService = ContentServiceFactory.getInstance().getHandleClarinService();
    private Handle handle;

    protected HandleBuilder(Context context) {
        super(context);
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            handle = c.reloadEntity(handle);
            delete(c, handle);
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public Handle build() throws SQLException, AuthorizeException {
        try {
            context.dispatchEvents();
            indexingService.commit();
            return handle;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public void delete(Context c, Handle dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    @Override
    protected HandleClarinService getService() {
        return handleClarinService;
    }

    public static HandleBuilder createExternalHandle(final Context context, final String handleStr,
                                                   final String handleUrl) {
        HandleBuilder builder = new HandleBuilder(context);
        return builder.create(context, handleStr, handleUrl);
    }

    private HandleBuilder create(final Context context, final String handleStr,
                               final String handleUrl) {
        this.context = context;

        try {
            handle = handleClarinService.createExternalHandle(context, handleStr, handleUrl);
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }
}
