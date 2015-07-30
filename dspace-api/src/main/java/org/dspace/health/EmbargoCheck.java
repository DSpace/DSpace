/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;


import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;
import org.dspace.embargo.EmbargoManager;

import java.sql.SQLException;

/**
 * @author LINDAT/CLARIN dev team
 */
public class EmbargoCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        String ret = "";
        Context context = null;
        try {
            context = new Context();
            ItemIterator item_iter = null;
            try {
                item_iter = EmbargoManager.getEmbargoedItems(context);
            } catch (IllegalArgumentException e) {
                error(e, "No embargoed items found");
            } catch (Exception e) {
                error(e);
            }

            while (item_iter != null && item_iter.hasNext()) {
                Item item = item_iter.next();
                String handle = item.getHandle();
                DCDate date = null;
                try {
                    date = EmbargoManager.getEmbargoTermsAsDate(context, item);
                } catch (Exception e) {
                }
                ret += String.format("%s embargoed till [%s]\n", handle,
                        date != null ? date.toString() : "null");
            }
            context.complete();
        } catch (SQLException e) {
            try {
                if ( null != context ) {
                    context.abort();
                }
            } catch (Exception e1) {
            }
        }

        return ret;
    }
}
