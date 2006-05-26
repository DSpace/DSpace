/*
 * Upgrade101To11.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.administer;

import org.dspace.content.DCDate;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * A command-line tool for performing necessary tweaks in the database for the
 * new last_modified column in the item table.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class Upgrade101To11
{
    /**
     * For invoking via the command line
     * 
     * @param argv
     *            command-line arguments
     */
    public static void main(String[] argv)
    {
        Context context = null;

        try
        {
            context = new Context();

            // Deal with withdrawn items first.
            // last_modified takes the value of the deletion date
            TableRowIterator tri = DatabaseManager.queryTable(context, "item",
                    "SELECT * FROM item WHERE withdrawal_date IS NOT NULL");

            while (tri.hasNext())
            {
                TableRow row = tri.next();
                DCDate d = new DCDate(row.getStringColumn("withdrawal_date"));
                row.setColumn("last_modified", d.toDate());
                DatabaseManager.update(context, row);
            }
            tri.close();

            // Next, update those items with a date.available
            tri = DatabaseManager.query(context,
                        "SELECT item.item_id, dcvalue.text_value FROM item, dctyperegistry, "+
                        "dcvalue WHERE item.item_id=dcvalue.item_id AND dcvalue.dc_type_id="+
                        "dctyperegistry.dc_type_id AND dctyperegistry.element LIKE 'date' "+
                        "AND dctyperegistry.qualifier LIKE 'available'");

            while (tri.hasNext())
            {
                TableRow resultRow = tri.next();
                DCDate d = new DCDate(resultRow.getStringColumn("text_value"));

                // Can't update the row, have to do a separate query
                TableRow itemRow = DatabaseManager.find(context, "item",
                        resultRow.getIntColumn("item_id"));
                itemRow.setColumn("last_modified", d.toDate());
                DatabaseManager.update(context, itemRow);
            }
            tri.close();

            // Finally, for all items that have no date.available or withdrawal
            // date, set the update time to now!
            DatabaseManager.updateQuery(context,
                        "UPDATE item SET last_modified=now() WHERE last_modified IS NULL");

            context.complete();

            System.out.println("Last modified dates set");

            System.exit(0);
        }
        catch (Exception e)
        {
            System.err.println("Exception occurred:" + e);
            e.printStackTrace();

            if (context != null)
            {
                context.abort();
            }

            System.exit(1);
        }
    }
}
