/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.security;

import java.sql.SQLException;
import java.util.Optional;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * QASecurity that restrict access to the QA Source and related events only to repository administrators
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.com)
 */
public class AdministratorsOnlyQASecurity implements QASecurity {

    @Autowired
    private AuthorizeService authorizeService;

    public Optional<String> generateFilterQuery(Context context, EPerson currentUser) {
        return Optional.empty();
    }

    @Override
    public boolean canSeeQASource(Context context, EPerson user) {
        try {
            return authorizeService.isAdmin(context, user);
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean canSeeQAEvent(Context context, EPerson user, QAEvent qaEvent) {
        try {
            return authorizeService.isAdmin(context, user);
        } catch (SQLException e) {
            return false;
        }
    }

}
