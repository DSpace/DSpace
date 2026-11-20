/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.wrapper;

import jakarta.servlet.http.HttpSession;
import org.apache.catalina.session.StandardSessionFacade;

/**
 * A custom {@link HttpSession} wrapper that allows to access to session fields in async way,
 * even after the session is not available anymore, i.e. the original request it's been recycled
 */
public class SavedHttpSessionWrapper extends StandardSessionFacade {
    private final String id;

    /**
     * Create an instance of {@link SavedHttpSessionWrapper}
     *
     * @param session the original {@link HttpSession}
     */
    public SavedHttpSessionWrapper(HttpSession session) {
        super(session);
        this.id = session != null ? session.getId() : null;
    }

    @Override
    public String getId() {
        return id;
    }
}
