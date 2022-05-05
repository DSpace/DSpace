/*
 * Copyright 2022 Indiana University.
 */
package org.dspace.app.statistics;

/**
 *
 * @author mwood
 */
public enum GetGithubReleaseOptions {
    HELP;

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

        options.addOption("v", "verbose", false, "Show lots of debugging information");

        return options;
    }

    protected static GetGithubReleaseOptions getOption(CommandLine command) {
        if (command.hasOption("h")) {
            return GetGithubReleaseOptions.HELP;
    }
}
