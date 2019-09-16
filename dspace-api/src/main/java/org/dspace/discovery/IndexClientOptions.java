/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.discovery;

import org.apache.commons.cli.CommandLine;

/**
 * This Enum holds all the possible options and combinations for the Index discovery script
 */
public enum IndexClientOptions {
    REMOVE,
    CLEAN,
    FORCECLEAN,
    BUILD,
    BUILDANDSPELLCHECK,
    OPTIMIZE,
    SPELLCHECK,
    INDEX,
    UPDATE,
    FORCEUPDATE,
    UPDATEANDSPELLCHECK,
    FORCEUPDATEANDSPELLCHECK,
    HELP;

    /**
     * This method resolves the CommandLine parameters to figure out which action the index-discovery script should
     * perform
     * @param commandLine   The relevant CommandLine for the index-discovery script
     * @return              The index-discovery option to be ran, parsed from the CommandLine
     */
    public static IndexClientOptions getIndexClientOption(CommandLine commandLine) {
        if (commandLine.hasOption("h")) {
            return IndexClientOptions.HELP;
        } else if (commandLine.hasOption("r")) {
            return IndexClientOptions.REMOVE;
        } else if (commandLine.hasOption("c")) {
            if (commandLine.hasOption("f")) {
                return IndexClientOptions.FORCECLEAN;
            } else {
                return IndexClientOptions.CLEAN;
            }
        } else if (commandLine.hasOption("b")) {
            if (commandLine.hasOption("s")) {
                return IndexClientOptions.BUILDANDSPELLCHECK;
            } else {
                return IndexClientOptions.BUILD;
            }
        } else if (commandLine.hasOption("o")) {
            return IndexClientOptions.OPTIMIZE;
        } else if (commandLine.hasOption("s")) {
            return IndexClientOptions.SPELLCHECK;
        } else if (commandLine.hasOption("i")) {
            return IndexClientOptions.INDEX;
        } else {
            if (commandLine.hasOption("f") && commandLine.hasOption("s")) {
                return IndexClientOptions.FORCEUPDATEANDSPELLCHECK;
            } else if (commandLine.hasOption("f")) {
                return IndexClientOptions.FORCEUPDATE;
            } else if (commandLine.hasOption("s")) {
                return IndexClientOptions.UPDATEANDSPELLCHECK;
            } else {
                return IndexClientOptions.UPDATE;
            }
        }
    }
}
