/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * "Script" options for {@link GetGithubReleaseOptions}.
 *
 * @author mwood
 */
public enum GetGithubReleaseOptions {
    GET,
    HELP;

    static final String OPT_HELP = "h";
    static final String OPT_FILE = "f";
    static final String OPT_OWNER = "o";
    static final String OPT_REPO = "r";
    static final String OPT_VERBOSE = "v";

    protected static Options constructOptions() {
        Options options = new Options();
        Option option;

        options.addOption(OPT_HELP, "help", false, "describe options");

        option = Option.builder(OPT_FILE)
                .longOpt("file")
                .hasArg()
                .hasArgs() // Repeatable
                .desc("path to extract from archive (repeatable)")
                .argName("path")
                .build();
        options.addOption(option);

        option = Option.builder(OPT_OWNER)
                .longOpt("owner")
                .hasArg()
                .desc("Owner of the repository")
                .required()
                .build();
        options.addOption(option);

        option = Option.builder(OPT_REPO)
                .longOpt("repository")
                .hasArg()
                .desc("Repository having the release")
                .required()
                .build();
        options.addOption(option);

        options.addOption(OPT_VERBOSE, "verbose", false, "Show lots of debugging information");

        return options;
    }
}
