/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.security;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Optional;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.qaevent.service.QAEventService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * QASecurity implementations that allow access to only qa events that match a SORL query generated using the eperson
 * uuid
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.com)
 */
public class UserBasedFilterQASecurity implements QASecurity {

    private String filterTemplate;
    private boolean allowAdmins = true;

    @Autowired
    private QAEventService qaEventService;
    @Autowired
    private AuthorizeService authorizeService;

    public Optional<String> generateFilterQuery(Context context, EPerson user) {
        try {
            if (allowAdmins && authorizeService.isAdmin(context, user)) {
                return Optional.empty();
            } else {
                return Optional.of(MessageFormat.format(filterTemplate, user.getID().toString()));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking permissions", e);
        }
    }

    public boolean canSeeQASource(Context context, EPerson user) {
        return user != null;
    }

    public boolean canSeeQAEvent(Context context, EPerson user, QAEvent qaEvent) {
        try {
            return (allowAdmins && authorizeService.isAdmin(context, user))
                    || qaEventService.qaEventsInSource(context, user, qaEvent.getEventId(), qaEvent.getSource());
        } catch (SQLException e) {
            throw new RuntimeException("Error checking permissions", e);
        }
    }

    public void setFilterTemplate(String filterTemplate) {
        this.filterTemplate = filterTemplate;
    }

    public void setAllowAdmins(boolean allowAdmins) {
        this.allowAdmins = allowAdmins;
    }

}
