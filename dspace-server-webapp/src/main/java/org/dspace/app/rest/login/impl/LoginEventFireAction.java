/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.login.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.login.PostLoggedInAction;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.EventService;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Implementation of {@link PostLoggedInAction} that fire an LOGIN event.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class LoginEventFireAction implements PostLoggedInAction {

    @Autowired
    private EventService eventService;

    @Override
    public void loggedIn(Context context) {

        HttpServletRequest request = getCurrentRequest();
        EPerson currentUser = context.getCurrentUser();

        eventService.fireEvent(new UsageEvent(UsageEvent.Action.LOGIN, request, context, currentUser));

    }

    private HttpServletRequest getCurrentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

}
