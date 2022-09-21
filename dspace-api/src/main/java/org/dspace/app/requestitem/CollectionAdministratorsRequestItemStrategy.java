/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.requestitem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.lang.NonNull;

/**
 * Derive request recipients from groups of the Collection which owns an Item.
 * The list will include all members of the administrators group.  If the
 * resulting list is empty, delegates to {@link RequestItemHelpdeskStrategy}.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class CollectionAdministratorsRequestItemStrategy
        extends RequestItemHelpdeskStrategy {
    @Override
    @NonNull
    public List<RequestItemAuthor> getRequestItemAuthor(Context context,
            Item item)
            throws SQLException {
        List<RequestItemAuthor> recipients = new ArrayList<>();
        Collection collection = item.getOwningCollection();
        for (EPerson admin : collection.getAdministrators().getMembers()) {
            recipients.add(new RequestItemAuthor(admin));
        }
        if (recipients.isEmpty()) {
            return super.getRequestItemAuthor(context, item);
        } else {
            return recipients;
        }
    }
}
