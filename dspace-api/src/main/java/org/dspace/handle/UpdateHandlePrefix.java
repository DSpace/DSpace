/*
 * UpdateHandlePrefix.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2008, Hewlett-Packard Company and Massachusetts
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
package org.dspace.handle;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.search.DSIndexer;
import org.dspace.browse.IndexBrowse;

/**
 * A sciprt to update the handle values in the database. This is typically used
 * when moving from a test machine (handle = 123456789) to a production service.
 *
 * @author Stuart Lewis
 */
public class UpdateHandlePrefix
{
    public static void main(String[] args) throws Exception
    {
        // There should be two paramters
        if (args.length < 2)
        {
            System.out.println("\nUsage: update-handle-prefix <old handle> <new handle>\n");
        }
        else
        {
            // Confirm with the user that this is what they want to do
            String oldH = args[0];
            String newH = args[1];

            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            Context context = new Context();
            System.out.println("If you continue, all handles in your repository with prefix " +
                                oldH + " will be updated to have handle prefix " + newH + "\n");
            String sql = "SELECT count(*) as count FROM handle " +
                         "WHERE handle LIKE '" + oldH + "%'";
            TableRow row = DatabaseManager.querySingle(context, sql, new Object[] {});
            long count = row.getLongColumn("count");
            System.out.println(count + " items will be updated.\n");
            System.out.print("Have you taken a backup, and are you ready to continue? [y/n]: ");
            String choiceString = input.readLine();

            if (choiceString.equalsIgnoreCase("y"))
            {
                // Make the changes
                System.out.print("Updating handle table... ");
                sql = "update handle set handle = '" + newH + "' || '/' || handle_id " +
                      "where handle like '" + oldH + "/%'";
                int updated = DatabaseManager.updateQuery(context, sql, new Object[] {});
                System.out.println(updated + " items updated");

                System.out.print("Updating metadatavalues table... ");
                sql = "UPDATE metadatavalue SET text_value= (SELECT 'http://hdl.handle.net/' || " +
                      "handle FROM handle WHERE handle.resource_id=item_id AND " +
                      "handle.resource_type_id=2) WHERE  text_value LIKE 'http://hdl.handle.net/%';";
                updated = DatabaseManager.updateQuery(context, sql, new Object[] {});
                System.out.println(updated + " metadata values updated");

                // Commit the changes
                context.complete();

                System.out.print("Re-creating browse and search indexes... ");                

                // Reinitialise the browse system
                IndexBrowse.main(new String[] {"-i"});

                // Reinitialise the browse system
                try
                {
                    DSIndexer.main(new String[0]);
                }
                catch (Exception e)
                {
                    // Not a lot we can do
                    System.out.println("Error re-indexing:");
                    e.printStackTrace();
                    System.out.println("\nPlease manually run [dspace]/bin/index-all");
                }

                // All done
                System.out.println("\nHandles successfully updated.");
            }
            else
            {
                System.out.println("No changes have been made to your data.");
            }
        }
    }
}