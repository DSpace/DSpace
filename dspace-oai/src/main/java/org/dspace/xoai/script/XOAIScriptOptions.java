/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.script;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class XOAIScriptOptions {
    public static final String IMPORT_ACTION = "import";
    public static final String CLEAN_CACHE_ACTION = "clean-cache";

    private XOAIScriptOptions() {}

    protected static String getAction(CommandLine commandLine) {
        final String action = commandLine.getOptionValue("a");
        if (action == null) {
            return null;
        } else if (action.equals(IMPORT_ACTION)) {
            return IMPORT_ACTION;
        } else if (action.equals(CLEAN_CACHE_ACTION)) {
            return CLEAN_CACHE_ACTION;
        } else {
            throw new IllegalArgumentException("Unknown action argument: " + action);
        }
    }

    protected static Options constructOptions() {
        Options options = new Options();
        options.addOption(
            "a",
            "action",
            true,
            "Action to perform, accepts 'import' or 'clean-cache'"
        );
        options.addOption(
            "c",
            "clear-index",
            false,
            "Clears the Solr index before indexing (it will import all items again)"
        );
        options.addOption(
            "v",
            "verbose",
            false,
            "Verbose output"
        );
        options.addOption(
            "h",
            "help",
            false,
            "Shows an help text"
        );
        return options;
    }
}
