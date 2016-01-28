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
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.discovery.IndexClient;

/**
 * A script to update the handle values in the database. This is typically used
 * when moving from a test machine (handle = 123456789) to a production service
 * or when make a test clone from production service.
 *
 * @author Stuart Lewis
 * @author Ivo Prajer (Czech Technical University in Prague)
 */
public class UpdateHandlePrefix
{

    private static final Logger log = Logger.getLogger(UpdateHandlePrefix.class);

    /**
     * When invoked as a command-line tool, updates handle prefix
     *
     * @param args the command-line arguments, none used
     * @throws java.lang.Exception
     *
     */
    public static void main(String[] args) throws Exception
    {
        // There should be two parameters
        if (args.length < 2)
        {
            System.out.println("\nUsage: update-handle-prefix <old handle> <new handle>\n");
            System.exit(1);
        }
        else
        {
            String oldH = args[0];
            String newH = args[1];

            // Get info about changes
            System.out.println("\nGetting information about handles from database...");
            Context context = new Context();
            String sql = "SELECT count(*) as count " +
                         "FROM handle " +
                         "WHERE handle LIKE '" + oldH + "%'";
            TableRow row = DatabaseManager.querySingle(context, sql, new Object[] {});
            long count = row.getLongColumn("count");

            if (count > 0)
            {
                // Print info text about changes
                System.out.println(
                  "In your repository will be updated " + count + " handle" +
                  ((count > 1) ? "s" : "") + " to new prefix " + newH +
                  " from original " + oldH + "!\n"
                );

                // Confirm with the user that this is what they want to do
                System.out.print(
                  "Servlet container (e.g. Apache Tomcat, Jetty, Caucho Resin) must be running.\n" +
                  "If it is necessary, please make a backup of the database.\n" +
                  "Are you ready to continue? [y/n]: "
                );
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                String choiceString = input.readLine();

                if (choiceString.equalsIgnoreCase("y"))
                {
                    try {
                        log.info("Updating handle prefix from " + oldH + " to " + newH);

                        // Make the changes
                        System.out.print("\nUpdating handle table... ");
                        sql = "UPDATE handle " +
                              "SET handle = '" + newH + "' || '/' || handle_id " +
                              "WHERE handle like '" + oldH + "/%'";
                        int updHdl = DatabaseManager.updateQuery(context, sql, new Object[] {});
                        System.out.println(
                          updHdl + " item" + ((updHdl > 1) ? "s" : "") + " updated"
                        );

                        System.out.print("Updating metadatavalues table... ");
                        sql = "UPDATE metadatavalue " +
                              "SET text_value = " +
                                "(" +
                                  "SELECT 'http://hdl.handle.net/' || handle " +
                                  "FROM handle " +
                                  "WHERE handle.resource_id = metadatavalue.resource_id " +
                                    "AND handle.resource_type_id = 2" +
                                ") " +
                              "WHERE text_value LIKE 'http://hdl.handle.net/" + oldH + "/%'" +
                                "AND EXISTS " +
                                  "(" +
                                    "SELECT 1 " +
                                    "FROM handle " + 
                                    "WHERE handle.resource_id = metadatavalue.resource_id " +
                                      "AND handle.resource_type_id = 2" +
                                  ")";
                        int updMeta = DatabaseManager.updateQuery(context, sql, new Object[] {});
                        System.out.println(
                          updMeta + " metadata value" + ((updMeta > 1) ? "s" : "") + " updated"
                        );

                        // Commit the changes
                        context.complete();

                        log.info(
                          "Done with updating handle prefix. " +
                          "It was changed " + updHdl + " handle" + ((updHdl > 1) ? "s" : "") +
                          " and " + updMeta + " metadata record" + ((updMeta > 1) ? "s" : "")
                        );

                    }
                    catch (SQLException sqle)
                    {
                        if ((context != null) && (context.isValid()))
                        {
                            context.abort();
                            context = null;
                        }
                        System.out.println("\nError during SQL operations.");
                        throw sqle;
                    }

                    System.out.println("Handles successfully updated in database.\n");
                    System.out.println("Re-creating browse and search indexes...");

                    try
                    {
                        // Reinitialise the search and browse system
                        IndexClient.main(new String[] {"-b"});
                        System.out.println("Browse and search indexes are ready now.");
                        // All done
                        System.out.println("\nAll done successfully. Please check the DSpace logs!\n");
                    }
                    catch (Exception e)
                    {
                        // Not a lot we can do
                        System.out.println("Error during re-indexing.");
                        System.out.println(
                          "\n\nAutomatic re-indexing failed. Please perform it manually.\n" +
                          "You should run one of the following commands:\n\n" +
                          "  [dspace]/bin/dspace index-discovery -b\n\n" +
                          "If you are using Solr for browse (this is the default setting).\n" +
                          "When launching this command, your servlet container must be running.\n\n" +
                          "  [dspace]/bin/dspace index-lucene-init\n\n" +
                          "If you enabled Lucene for search.\n" +
                          "When launching this command, your servlet container must be shutdown.\n"
                        );
                        throw e;
                    }
                }
                else
                {
                    System.out.println("No changes have been made to your data.\n");
                }
            }
            else
            {
                System.out.println("Nothing to do! All handles are up-to-date.\n");
            }
        }
    }
}