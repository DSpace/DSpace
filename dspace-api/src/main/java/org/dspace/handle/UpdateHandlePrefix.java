/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
 * A script to update the handle values in the database. This is typically used
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