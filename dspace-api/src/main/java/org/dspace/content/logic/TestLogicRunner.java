/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * A command-line runner used for testing a logical filter against an item, or all items
 *
 * @author Kim Shepherd
 */
public class TestLogicRunner {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(TestLogicRunner.class);

    /**
     * Default constructor
     */
    private TestLogicRunner() { }

    /**
     * Main runner method for CLI usage
     * @param argv  array of command-line arguments
     */
    public static void main(String[] argv) {
        System.out.println("Starting impl of main() test spring logic item filter");

        // initialize options
        Options options = new Options();

        options.addOption("h", "help", false, "Help");
        options.addOption("l", "list", false, "List filters");
        options.addOption("f", "filter", true, "Use filter <filter>");
        options.addOption("i","item", true, "Run filter over item <handle>");
        options.addOption("a","all", false, "Run filter over all items");

        // initialize parser
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;
        HelpFormatter helpformater = new HelpFormatter();

        try {
            line = parser.parse(options, argv);
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }

        if (line.hasOption("help")) {
            helpformater.printHelp("\nTest the DSpace logical item filters\n", options);
            System.exit(0);
        }

        // Create a context
        Context c = new Context(Context.Mode.READ_ONLY);
        //c.turnOffAuthorisationSystem();
        ServiceManager manager = DSpaceServicesFactory.getInstance().getServiceManager();

        if (line.hasOption("list")) {
            // Lit filters and exit
            List<Filter> filters = manager.getServicesByType(Filter.class);
            for (Filter filter : filters) {
                System.out.println(filter.getClass().toString());
            }
            System.out.println("See item-filters.xml spring config for filter names");
            System.exit(0);
        }

        Filter filter;

        if (line.hasOption("filter")) {
            String filterName = line.getOptionValue("filter");
            filter = manager.getServiceByName(filterName, Filter.class);
            if (filter == null) {
                System.out.println("Error loading filter: " + filterName);
                System.exit(1);
            }

            if (line.hasOption("item")) {
                String handle = line.getOptionValue("item");

                HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
                try {
                    DSpaceObject dso = handleService.resolveToObject(c, handle);
                    if (Constants.typeText[dso.getType()].equals("ITEM")) {
                        Item item = (Item) dso;
                        System.out.println(filter.getResult(c, item));
                    } else {
                        System.out.println(handle + " is not an ITEM");
                    }
                } catch (SQLException | LogicalStatementException e) {
                    System.out.println("Error encountered processing item " + handle + ": " + e.getMessage());
                }

            } else if (line.hasOption("all")) {
                ItemService itemService = ContentServiceFactory.getInstance().getItemService();
                try {
                    Iterator<Item> itemIterator = itemService.findAll(c);
                    while (itemIterator.hasNext()) {
                        Item i = itemIterator.next();
                        System.out.println(
                            "Testing '" + filter + "' on item " + i.getHandle() + " ('" + i.getName() + "')"
                        );
                        System.out.println(filter.getResult(c, i));

                    }
                } catch (SQLException | LogicalStatementException e) {
                    System.out.println("Error encountered processing items: " + e.getMessage());
                }
            } else {
                helpformater.printHelp("\nTest the DSpace logical item filters\n", options);
            }
        }

    }

}