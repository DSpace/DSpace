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
import java.util.List;
import org.apache.log4j.Logger;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexClient;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

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
            HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
  
            String oldH = args[0];
            String newH = args[1];

            // Get info about changes
            System.out.println("\nGetting information about handles from database...");
            Context context = new Context();

            long count = handleService.countHandlesByPrefix(context, oldH);

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
                	context.turnOffAuthorisationSystem();
                    try {
                        log.info("Updating handle prefix from " + oldH + " to " + newH);

                        // Make the changes
                        System.out.print("\nUpdating handle table... ");
                        int updHdl = handleService.updateHandlesWithNewPrefix(context, newH, oldH);
                        System.out.println(
                          updHdl + " item" + ((updHdl > 1) ? "s" : "") + " updated"
                        );

                        System.out.print("Updating metadatavalues table... ");
                        MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
                        List<MetadataValue> metadataValues = metadataValueService.findByValueLike(context, "http://hdl.handle.net/");
                        int updMeta = metadataValues.size();
                        for (MetadataValue metadataValue : metadataValues) {
                        	metadataValue.setValue(metadataValue.getValue().replace("http://hdl.handle.net/" + oldH, "http://hdl.handle.net/" + newH));
                            metadataValueService.update(context, metadataValue, true);
                        }
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
                          "\n\nAutomatic re-indexing failed. Please perform it manually.\n\n" +
                          "  [dspace]/bin/dspace index-discovery -b\n\n" +
                          "When launching this command, your servlet container must be running.\n"
                        );
                        throw e;
                    }
                    context.restoreAuthSystemState();
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