/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Configuration tools.
 *
 * @author mwood
 */
public class Configuration {

    /**
     * Default constructor
     */
    private Configuration() { }

    /**
     * Command-line interface for running configuration tasks. Possible
     * arguments:
     * <ul>
     * <li>{@code --module name} the name of the configuration "module" for this property.</li>
     * <li>{@code --property name} prints the value of the DSpace configuration
     * property {@code name} to the standard output.</li>
     * <li>{@code --raw} suppresses parameter substitution in the output.</li>
     * <li>{@code --first} print only the first of multiple values.</li>
     * <li>{@code --help} describes these options.</li>
     * </ul>
     * If the property does not exist, nothing is written.
     *
     * @param argv the command line arguments given
     */
    public static void main(String[] argv) {
        // Build a description of the command line
        Options options = new Options();
        options.addOption("p", "property", true, "name of the desired property");
        options.addOption("m", "module", true,
                          "optional name of the module in which 'property' exists");
        options.addOption("r", "raw", false,
                          "do not do property substitution on the value");
        options.addOption("f", "first", false,
                          "display only the first value of an array property");
        options.addOption("?", "Get help");
        options.addOption("h", "help", false, "Get help");

        // Analyze the command line
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, argv);
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        // Give help if asked
        if (cmd.hasOption('?') || cmd.hasOption('h')) {
            new HelpFormatter().printHelp("dsprop [options]",
                                          "Display the value of a DSpace configuration property",
                                          options,
                                          "If --module is omitted, then --property gives the entire" +
                                              " name of the property.  Otherwise the name is" +
                                              " composed of module.property.");
            System.exit(0);
        }

        // Check for missing required values
        if (!cmd.hasOption('p')) {
            System.err.println("Error:  -p is required");
            System.exit(1);
        }

        // Figure out the property's full name
        StringBuilder propNameBuilder = new StringBuilder(1024);
        if (cmd.hasOption('m')) {
            propNameBuilder.append(cmd.getOptionValue('m'))
                           .append('.');
        }
        propNameBuilder.append(cmd.getOptionValue('p'));
        String propName = propNameBuilder.toString();

        // Print the property's value(s), if it exists
        ConfigurationService cfg = DSpaceServicesFactory.getInstance().getConfigurationService();
        if (!cfg.hasProperty(propName)) {
            System.out.println();
        } else {
            if (cmd.hasOption('r')) {
                // Print "raw" values (without property substitutions)
                Object rawValue = cfg.getPropertyValue(propName);
                if (rawValue.getClass().isArray()) {
                    for (Object value : (Object[]) rawValue) {
                        System.out.println(value.toString());
                        if (cmd.hasOption('f')) {
                            break; // If --first print only one value
                        }
                    }
                } else { // Not an array
                    System.out.println(rawValue.toString());
                }
            } else {
                // Print values with property substitutions
                String[] values = cfg.getArrayProperty(propName);
                for (String value : values) {
                    System.out.println(value);
                    if (cmd.hasOption('f')) {
                        break; // If --first print only one value
                    }
                }
            }
        }

        System.exit(0);
    }
}
