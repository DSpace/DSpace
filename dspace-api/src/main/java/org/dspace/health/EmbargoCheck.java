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
import org.dspace.core.Context;
import org.dspace.embargo.factory.EmbargoServiceFactory;
import org.dspace.embargo.service.EmbargoService;

import java.sql.SQLException;
import java.util.Iterator;

/**
 * @author LINDAT/CLARIN dev team
 */
public class EmbargoCheck extends Check {

    private static final EmbargoService embargoService = EmbargoServiceFactory.getInstance().getEmbargoService();

    @Override
    public String run( ReportInfo ri ) {
        String ret = "";
        Context context = null;
        try {
            context = new Context();
            Iterator<Item> item_iter = null;
            try {
                item_iter = embargoService.findItemsByLiftMetadata(context);
            } catch (IllegalArgumentException e) {
                error(e, "No embargoed items found");
		ret += "Note: This check is for pre-3.0 embargo functionality.\n";
		ret += "If you aren't using it, you can ignore this error.\n";
            } catch (Exception e) {
                error(e);
            }

            while (item_iter != null && item_iter.hasNext()) {
                Item item = item_iter.next();
                String handle = item.getHandle();
                DCDate date = null;
                try {
                    date = embargoService.getEmbargoTermsAsDate(context, item);
                } catch (Exception e) {
                    error(e);
                }
                ret += String.format("%s embargoed till [%s]\n", handle,
                        date != null ? date.toString() : "null");
            }
            context.complete();
        } catch (SQLException e) {
            error(e);
            try {
                if ( null != context ) {
                    context.abort();
                }
            } catch (Exception e1) {
                error(e);
            }
        }

        return ret;
    }
}
