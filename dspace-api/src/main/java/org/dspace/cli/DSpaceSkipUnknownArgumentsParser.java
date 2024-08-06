/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Extended version of the DefaultParser. This parser skip/ignore unknown arguments.
 */
public class DSpaceSkipUnknownArgumentsParser extends DefaultParser {


    @Override
    public CommandLine parse(Options options, String[] arguments) throws ParseException {
        return super.parse(options, getOnlyKnownArguments(options, arguments));
    }

    @Override
    public CommandLine parse(Options options, String[] arguments, Properties properties) throws ParseException {
        return super.parse(options, getOnlyKnownArguments(options, arguments), properties);
    }

    /**
     * Parse the arguments according to the specified options and properties.
     * @param options the specified Options
     * @param arguments the command line arguments
     * @param stopAtNonOption can be ignored - an unrecognized argument is ignored, an unrecognized argument doesn't
     *                        stop the parsing and doesn't trigger a ParseException
     *
     * @return the list of atomic option and value tokens
     * @throws ParseException if there are any problems encountered while parsing the command line tokens.
     */
    @Override
    public CommandLine parse(Options options, String[] arguments, boolean stopAtNonOption) throws ParseException {
        return super.parse(options, getOnlyKnownArguments(options, arguments), stopAtNonOption);
    }

    /**
     * Parse the arguments according to the specified options and properties.
     * @param options         the specified Options
     * @param arguments       the command line arguments
     * @param properties      command line option name-value pairs
     * @param stopAtNonOption can be ignored - an unrecognized argument is ignored, an unrecognized argument doesn't
     *                        stop the parsing and doesn't trigger a ParseException
     *
     * @return the list of atomic option and value tokens
     * @throws ParseException if there are any problems encountered while parsing the command line tokens.
     */
    @Override
    public CommandLine parse(Options options, String[] arguments, Properties properties, boolean stopAtNonOption)
            throws ParseException {
        return super.parse(options, getOnlyKnownArguments(options, arguments), properties, stopAtNonOption);
    }


    private String[] getOnlyKnownArguments(Options options, String[] arguments) {
        List<String> knownArguments = new ArrayList<>();
        for (String arg : arguments) {
            if (options.hasOption(arg)) {
                knownArguments.add(arg);
            }
        }
        return knownArguments.toArray(new String[0]);
    }
}
